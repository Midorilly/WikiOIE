/**
 * Copyright (c) 2021, the WikiOIE AUTHORS.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the University of Bari nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * GNU GENERAL PUBLIC LICENSE - Version 3, 29 June 2007
 *
 */
package di.uniba.it.wikioie;

import di.uniba.it.wikioie.data.Pair;
import di.uniba.it.wikioie.data.Span;
import di.uniba.it.wikioie.data.Token;
import di.uniba.it.wikioie.data.Triple;
import di.uniba.it.wikioie.training.CoTraining;
import di.uniba.it.wikioie.training.FileInstance;
import di.uniba.it.wikioie.training.Instance;
import di.uniba.it.wikioie.training.TrainingSet;
import di.uniba.it.wikioie.udp.UDPSentence;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.jgrapht.Graph;

/**
 *
 * @author pierpaolo
 */
public class Utils {

    private static final Logger LOG = Logger.getLogger(Utils.class.getName());

    /**
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static String readText(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        while (reader.ready()) {
            sb.append(reader.readLine());
            sb.append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String readText(File file) throws IOException {
        return readText(new FileInputStream(file));
    }

    /**
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public static String readText(String filename) throws IOException {
        return readText(new FileInputStream(filename));
    }

    /**
     *
     * @param file
     * @param minOcc
     * @return
     * @throws IOException
     */
    public static Set<String> loadFilterSet(File file, int minOcc) throws IOException {
        Set<String> set = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            String[] v = reader.readLine().split("\t");
            if (Integer.parseInt(v[1]) < minOcc) {
                set.add(v[0]);
            }
        }
        reader.close();
        LOG.log(Level.INFO, "Filtered {0} items from {1}", new Object[]{set.size(), file.getName()});
        return set;
    }

    /**
     *
     * @param sentence
     * @param span
     * @return
     */
    public static Pair<String, Set<String>> getPosFeature(UDPSentence sentence, Span span) {
        Set<String> set = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (int i = span.getStart(); i < span.getEnd(); i++) {
            sb.append("_").append(sentence.getTokens().get(i).getUpostag());
            set.add(sentence.getTokens().get(i).getUpostag());
        }
        return new Pair(sb.toString(), set);
    }

    /**
     *
     * @param sentence
     * @param dep
     * @param head
     * @return
     */
    public static Set<String> getDependencies(UDPSentence sentence, Span dep, Span head) {
        Set<String> set = new HashSet<>();
        Graph<Token, String> graph = sentence.getGraph();
        for (int i = dep.getStart(); i < dep.getEnd(); i++) {
            Set<String> edges = graph.outgoingEdgesOf(sentence.getTokens().get(i));
            for (String edge : edges) {
                Token target = graph.getEdgeTarget(edge);
                if (target.getId() >= head.getStart() && target.getId() < head.getEnd()) {
                    set.add("PD_" + target.getDepRel());
                }
            }
        }
        return set;
    }

    /**
     *
     * @param inputfile
     * @param outfile
     * @throws IOException
     */
    public static void copyByLine(File inputfile, File outfile) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(inputfile));
        BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
        while (in.ready()) {
            out.write(in.readLine());
            out.newLine();
        }
        in.close();
        out.close();
    }

    /**
     *
     * @param dict
     * @param outputfile
     * @throws IOException
     */
    public static void saveDict(Map<String, Integer> dict, File outputfile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputfile));
        for (String key : dict.keySet()) {
            writer.append(key).append("\t").append(dict.get(key).toString());
            writer.newLine();
        }
        writer.close();
    }

    /**
     *
     * @param bootfile
     * @param datafile
     * @param outfile
     * @throws IOException
     */
    public static void removeDuplicate(File bootfile, File datafile, File outfile) throws IOException {
        Reader in = new FileReader(bootfile);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
        Map<String, List<FileInstance>> bootins = new HashMap<>();
        int id = 0;
        for (CSVRecord record : records) {
            String title = record.get("title");
            List<FileInstance> l = bootins.get(title);
            if (l == null) {
                l = new ArrayList<>();
                bootins.put(title, l);
            }
            FileInstance inst = new FileInstance(id, record.get("text"), record.get("subject"), record.get("predicate"), record.get("object"));
            l.add(inst);
            id++;
        }
        in.close();

        Set<Integer> ids = new HashSet<>();
        in = new FileReader(datafile);
        records = CSVFormat.TDF.withFirstRecordAsHeader().parse(in);
        int r = 1; // first line is the header!!!
        for (CSVRecord record : records) {
            List<FileInstance> l = bootins.get(record.get("title"));
            if (l != null) {
                for (FileInstance inst : l) {
                    if (inst.getText().equals(record.get("text")) && inst.getSubject().equals(record.get("subject")) && inst.getPredicate().equals(record.get("predicate")) && inst.getObject().equals(record.get("object"))) {
                        ids.add(r);
                        System.out.println("Duplicate at: " + r);
                        break;
                    }
                }
            }
            r++;
        }
        in.close();

        BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));
        in = new FileReader(datafile);
        BufferedReader reader = new BufferedReader(in);
        r = 0;
        while (reader.ready()) {
            String line = reader.readLine();
            if (!ids.contains(r)) {
                writer.write(line);
                writer.newLine();
            }
            r++;
        }
        reader.close();
        writer.close();
    }

    /**
     *
     * @param sentence
     * @param triple
     * @return
     */
    public static boolean invertTriple(UDPSentence sentence, Triple triple) {
        boolean subjIsObj = false;
        boolean subjIsSubj = false;
        for (int i = triple.getSubject().getStart(); i < triple.getSubject().getEnd(); i++) {
            if (sentence.getTokens().get(i).getDepRel().contains("obj")) {
                subjIsObj = true;
            } else if (sentence.getTokens().get(i).getDepRel().contains("subj")) {
                subjIsSubj = true;
            }
        }
        boolean objIsSubj = false;
        boolean objIsObj = false;
        for (int i = triple.getObject().getStart(); i < triple.getObject().getEnd(); i++) {
            if (sentence.getTokens().get(i).getDepRel().contains("subj")) {
                objIsSubj = true;
            } else if (sentence.getTokens().get(i).getDepRel().contains("obj")) {
                objIsObj = true;
            }
        }
        if ((subjIsObj && !subjIsSubj) && (objIsSubj && !objIsObj)) {
            Span a = triple.getSubject();
            triple.setSubject(triple.getObject());
            triple.setObject(a);
            return true;
        } else {
            return false;
        }
    }

}
