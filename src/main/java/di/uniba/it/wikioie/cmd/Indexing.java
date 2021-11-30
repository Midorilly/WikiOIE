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
package di.uniba.it.wikioie.cmd;

import di.uniba.it.wikioie.Utils;
import di.uniba.it.wikioie.indexing.WikiOIEIndex;
import di.uniba.it.wikioie.indexing.post.PassageProcessor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This class indexes a JSON dump with triples.
 *
 * @author pierpaolo
 */
public class Indexing {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Options options = new Options();
        options = options.addOption(new Option("i", true, "Input directory"))
                .addOption(new Option("o", true, "Output directory"))
                .addOption(new Option("f", true, "Filter file, (optional)"))
                .addOption(new Option("m", true, "Min occurrances (optional, default 5)"))
                .addOption(new Option("p", true, "Post processing class (optional)"))
                .addOption(new Option("t", true, "Training file (optional)"))
                .addOption(new Option("c", true, "C value (optional, default=1)"));
        try {
            DefaultParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("i") && cmd.hasOption("o")) {
                WikiOIEIndex idx = new WikiOIEIndex();
                PassageProcessor processor = null;
                if (cmd.hasOption("p")) {
                    try {
                        if (cmd.hasOption("t")) {
                            processor = (PassageProcessor) ClassLoader.getSystemClassLoader().loadClass("di.uniba.it.wikioie.indexing.post." + cmd.getOptionValue("p")).getDeclaredConstructor(File.class, Double.class).newInstance(new File(cmd.getOptionValue("t")), Double.parseDouble(cmd.getOptionValue("c", "1")));
                        } else {
                            processor = (PassageProcessor) ClassLoader.getSystemClassLoader().loadClass("di.uniba.it.wikioie.indexing.post." + cmd.getOptionValue("p")).getDeclaredConstructor().newInstance();
                        }
                    } catch (ClassNotFoundException | NoSuchMethodException ex) {
                        Logger.getLogger(Indexing.class.getName()).log(Level.SEVERE, "Not valid processor, use null", ex);
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        Logger.getLogger(Indexing.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                Set<String> filter = new HashSet<>();
                if (cmd.hasOption("f")) {
                    filter = Utils.loadFilterSet(new File(cmd.getOptionValue("f")), Integer.parseInt(cmd.getOptionValue("m", "5")));
                }
                idx.index(cmd.getOptionValue("i"), cmd.getOptionValue("o"), filter, processor);
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("WikiOIE - Run indexing", options);
            }
        } catch (IOException ex) {
            Logger.getLogger(Indexing.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("WikiOIE Run indexing", options);
        }
    }

}
