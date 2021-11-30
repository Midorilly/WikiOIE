/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.wikioie.training;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import di.uniba.it.wikioie.Utils;
import di.uniba.it.wikioie.data.Config;
import di.uniba.it.wikioie.data.Pair;
import di.uniba.it.wikioie.data.Triple;
import di.uniba.it.wikioie.process.WikiExtractor;
import di.uniba.it.wikioie.process.WikiITSimpleDepExtractor;
import di.uniba.it.wikioie.udp.UDPParser;
import di.uniba.it.wikioie.udp.UDPSentence;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * @author pierpaolo
 */
public class CoTraining {

    private double thPred = 0.85;
    
    private SolverType solver = SolverType.L2R_LR;

    private static final Logger LOG = Logger.getLogger(CoTraining.class.getName());

    /**
     *
     */
    public enum BootstrappingHeaders {

        /**
         *
         */
        title,

        /**
         *
         */
        text,

        /**
         *
         */
        score,

        /**
         *
         */
        subject,

        /**
         *
         */
        predicate,

        /**
         *
         */
        object,

        /**
         *
         */
        label
    }

    private Pair<UDPSentence, Triple> ieprocessing(String text, UDPParser parser, WikiExtractor extractor, String subj, String pred, String obj) throws IOException {
        List<UDPSentence> sentences = parser.getSentences(text);
        for (UDPSentence s : sentences) {
            List<Triple> ts = extractor.extract(s.getGraph());
            for (Triple t : ts) {
                if (t.getSubject().getSpan().equals(subj) && t.getPredicate().getSpan().equals(pred) && t.getObject().getSpan().equals(obj)) {
                    return new Pair<>(s, t);
                }
            }
        }
        return null;
    }

    /**
     *
     * @param pair
     * @return
     */
    public Set<String> generateFeatureSet(Pair<UDPSentence, Triple> pair) {
        Set<String> set = new HashSet<>();
        Triple triple = pair.getB();
        Pair<String, Set<String>> pf = Utils.getPosFeature(pair.getA(), triple.getSubject());
        set.add("pos_subj" + pf.getA());
        for (String pos : pf.getB()) {
            set.add("pos_subj_t_" + pos);
        }
        pf = Utils.getPosFeature(pair.getA(), triple.getPredicate());
        set.add("pos_pred" + pf.getA());
        for (String pos : pf.getB()) {
            set.add("pos_pred_t_" + pos);
        }
        pf = Utils.getPosFeature(pair.getA(), triple.getObject());
        set.add("pos_obj" + pf.getA());
        for (String pos : pf.getB()) {
            set.add("pos_obj_t_" + pos);
        }

        set.add("pred_span_" + triple.getPredicate().getSpan().toLowerCase());

        Set<String> dependencies = Utils.getDependencies(pair.getA(), pair.getB().getSubject(), pair.getB().getPredicate());
        for (String f : dependencies) {
            set.add("S_" + f);
        }
        dependencies = Utils.getDependencies(pair.getA(), pair.getB().getObject(), pair.getB().getPredicate());
        for (String f : dependencies) {
            set.add("O_" + f);
        }
        return set;
    }

    private List<FileInstance> loadUnlabelled(File inputFile) throws IOException {
        List<FileInstance> list = new ArrayList<>();
        Reader in = new FileReader(inputFile);
        Iterable<CSVRecord> records = CSVFormat.TDF.withFirstRecordAsHeader().parse(in);
        int id = 0;
        for (CSVRecord record : records) {
            list.add(new FileInstance(id, record.get("text"),
                    record.get("subject"),
                    record.get("predicate"),
                    record.get("object"),
                    Float.parseFloat(record.get("score"))));
            id++;
        }
        return list;
    }

    private List<FileInstance> loadUnlabelledCSV(File inputFile) throws IOException {
        List<FileInstance> list = new ArrayList<>();
        Reader in = new FileReader(inputFile);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
        int id = 0;
        for (CSVRecord record : records) {
            list.add(new FileInstance(id, record.get("text"),
                    record.get("subject"),
                    record.get("predicate"),
                    record.get("object"),
                    Float.parseFloat(record.get("score"))));
            id++;
        }
        return list;
    }

    /**
     *
     * @param inputFile
     * @param parser
     * @param extractor
     * @return
     * @throws IOException
     */
    public TrainingSet generateFeatures(File inputFile, UDPParser parser, WikiExtractor extractor) throws IOException {
        Reader in = new FileReader(inputFile);
        TrainingSet tr = new TrainingSet();
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(BootstrappingHeaders.class).withFirstRecordAsHeader().parse(in);
        int id = 0;
        for (CSVRecord record : records) {
            String text = record.get(BootstrappingHeaders.text);
            Pair<UDPSentence, Triple> pair = ieprocessing(text, parser, extractor, record.get(BootstrappingHeaders.subject), record.get(BootstrappingHeaders.predicate), record.get(BootstrappingHeaders.object));
            if (pair == null) {
                //LOG.warning("No triple");
            } else {
                Set<String> fset = generateFeatureSet(pair);
                Instance inst = new Instance(id);
                for (String v : fset) {
                    int fid = tr.addFeature(v);
                    inst.setFeature(fid, 1);
                }
                int sid = tr.addFeature("subj_score");
                inst.setFeature(sid, pair.getB().getSubject().getScore());
                sid = tr.addFeature("obj_score");
                inst.setFeature(sid, pair.getB().getObject().getScore());
                sid = tr.addFeature("t_score");
                inst.setFeature(sid, pair.getB().getScore());
                inst.setLabel(Integer.parseInt(record.get(BootstrappingHeaders.label)));
                tr.addInstance(inst);
            }
            id++;
        }
        in.close();
        return tr;
    }

    /**
     *
     * @param file
     * @return
     * @throws IOException
     */
    public List<Integer> loadLabels(File file) throws IOException {
        List<Integer> labels = new ArrayList<>();
        FileReader in = new FileReader(file);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(BootstrappingHeaders.class).withFirstRecordAsHeader().parse(in);
        for (CSVRecord record : records) {
            labels.add(Integer.parseInt(record.get(BootstrappingHeaders.label)));
        }
        in.close();
        return labels;
    }

    /**
     *
     * @param map
     * @param ts
     * @param parser
     * @param extractor
     * @return
     * @throws IOException
     */
    public TrainingSet generateTestFeatures(Map<String, Integer> map, List<FileInstance> ts, UDPParser parser, WikiExtractor extractor) throws IOException {
        TrainingSet tr = new TrainingSet(map);
        int id = 0;
        for (FileInstance finst : ts) {
            String text = finst.getText();
            Pair<UDPSentence, Triple> pair = ieprocessing(text, parser, extractor, finst.getSubject(), finst.getPredicate(), finst.getObject());
            if (pair == null) {
                //LOG.warning("No triple");
            } else {
                Set<String> fset = generateFeatureSet(pair);
                Instance inst = new Instance(id);
                for (String v : fset) {
                    Integer fid = tr.getId(v);
                    if (fid != null) {
                        inst.setFeature(fid, 1);
                    }
                }
                int sid = tr.getId("subj_score");
                inst.setFeature(sid, pair.getB().getSubject().getScore());
                sid = tr.getId("obj_score");
                inst.setFeature(sid, pair.getB().getObject().getScore());
                sid = tr.getId("t_score");
                inst.setFeature(sid, pair.getB().getScore());
                tr.addInstance(inst);
            }
            id++;
        }
        return tr;
    }

    /**
     *
     * @param ts
     * @param outputfile
     * @throws IOException
     */
    public void saveTr(TrainingSet ts, File outputfile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputfile));
        List<Integer> ids = new ArrayList<>(ts.getDict().values());
        Collections.sort(ids);
        for (Instance i : ts.getSet()) {
            writer.append(String.valueOf(i.getLabel()));
            for (Integer id : ids) {
                float v = i.getFeature(id);
                if (v != 0) {
                    writer.append(" ");
                    writer.append(id.toString()).append(":").append(String.valueOf(v));
                }
            }
            writer.newLine();
        }
        writer.close();
    }

    /**
     *
     * @param ts
     * @param C
     * @return
     */
    public Model train(TrainingSet ts, double C) {
        List<Integer> ids = new ArrayList<>(ts.getDict().values());
        Collections.sort(ids);
        Problem problem = new Problem();
        problem.l = ts.getSet().size();
        problem.n = ts.getDict().size();
        Feature[][] x = new Feature[problem.l][];
        double[] y = new double[problem.l];
        for (int k = 0; k < ts.getSet().size(); k++) {
            Instance inst = ts.getSet().get(k);
            x[k] = new Feature[inst.getFeatures().size()];
            int j = 0;
            for (Integer id : ids) {
                float v = inst.getFeature(id);
                if (v != 0) {
                    x[k][j] = new FeatureNode(id, v);
                    j++;
                }
            }
            y[k] = inst.getLabel();
        }
        problem.x = x;
        problem.y = y;
        double eps = 0.1;
        Parameter parameter = new Parameter(solver, C, eps);
        Model model = Linear.train(problem, parameter);
        return model;
    }

    /**
     *
     * @param ts
     * @param k
     * @param C
     */
    @Deprecated
    public void kfold(TrainingSet ts, int k, double C) {
        List<Integer> ids = new ArrayList<>(ts.getDict().values());
        Collections.sort(ids);
        Problem problem = new Problem();
        problem.l = ts.getSet().size();
        problem.n = ts.getDict().size();
        Feature[][] x = new Feature[problem.l][];
        double[] y = new double[problem.l];
        for (int l = 0; l < ts.getSet().size(); l++) {
            Instance inst = ts.getSet().get(l);
            x[l] = new Feature[inst.getFeatures().size()];
            int j = 0;
            for (Integer id : ids) {
                float v = inst.getFeature(id);
                if (v != 0) {
                    x[l][j] = new FeatureNode(id, v);
                    j++;
                }
            }
            y[l] = inst.getLabel();
        }
        problem.x = x;
        problem.y = y;
        double eps = 0.1;
        Parameter parameter = new Parameter(solver, C, eps);
        double[] p = new double[y.length];
        Linear.crossValidation(problem, parameter, k, p);
        List<Integer> labels = new ArrayList<>();
        for (double v : y) {
            labels.add((int) Math.round(v));
        }
        List<Integer> pred = new ArrayList<>();
        for (double v : p) {
            pred.add((int) Math.round(v));
        }
        computeMetrics(labels, pred);
    }

    /**
     *
     * @param model
     * @param ts
     * @return
     */
    public List<Pair<Integer, Integer>> testWithProb(Model model, TrainingSet ts) {
        List<Pair<Integer, Integer>> r = new ArrayList<>();
        List<Integer> ids = new ArrayList<>(ts.getDict().values());
        Collections.sort(ids);
        for (int k = 0; k < ts.getSet().size(); k++) {
            Instance inst = ts.getSet().get(k);
            Feature[] fx = new Feature[inst.getFeatures().size()];
            int j = 0;
            for (Integer id : ids) {
                float v = inst.getFeature(id);
                if (v != 0) {
                    fx[j] = new FeatureNode(id, v);
                    j++;
                }
            }
            double[] prob = new double[2];
            double l = Linear.predictProbability(model, fx, prob);
            //System.out.println(l + "\t" + Arrays.toString(prob));
            if (prob[0] >= thPred || prob[1] >= thPred) {
                r.add(new Pair<>(inst.getId(), (int) Math.round(l)));
            }
        }
        return r;
    }

    /**
     *
     * @param model
     * @param ts
     * @return
     */
    public List<Integer> test(Model model, TrainingSet ts) {
        List<Integer> r = new ArrayList<>();
        List<Integer> ids = new ArrayList<>(ts.getDict().values());
        Collections.sort(ids);
        for (int k = 0; k < ts.getSet().size(); k++) {
            Instance inst = ts.getSet().get(k);
            Feature[] fx = new Feature[inst.getFeatures().size()];
            int j = 0;
            for (Integer id : ids) {
                float v = inst.getFeature(id);
                if (v != 0) {
                    fx[j] = new FeatureNode(id, v);
                    j++;
                }
            }
            double l = Linear.predict(model, fx);
            r.add((int) Math.round(l));
        }
        return r;
    }

    /**
     *
     * @param model
     * @param ts
     * @return
     */
    public List<Pair<Integer, Integer>> testWithoutProb(Model model, TrainingSet ts) {
        List<Pair<Integer, Integer>> r = new ArrayList<>();
        List<Integer> ids = new ArrayList<>(ts.getDict().values());
        Collections.sort(ids);
        for (int k = 0; k < ts.getSet().size(); k++) {
            Instance inst = ts.getSet().get(k);
            Feature[] fx = new Feature[inst.getFeatures().size()];
            int j = 0;
            for (Integer id : ids) {
                float v = inst.getFeature(id);
                if (v != 0) {
                    fx[j] = new FeatureNode(id, v);
                    j++;
                }
            }
            double l = Linear.predict(model, fx);
            r.add(new Pair(inst.getId(), (int) Math.round(l)));
        }
        return r;
    }

    private double computeBalance(TrainingSet ts) {
        double n = 0;
        for (Instance inst : ts.getSet()) {
            if (inst.getLabel() == 1) {
                n++;
            }
        }
        return n / (double) ts.getSet().size();
    }

    private void balance(List<Pair<Integer, Integer>> r, double bal) {
        Collections.sort(r, new PairBComparator());
        double n1 = 0;
        for (Pair<Integer, Integer> p : r) {
            if (p.getB() == 1) {
                n1++;
            }
        }
        double newbal = n1 / (double) r.size();
        if (newbal > bal) {
            while (newbal > bal) {
                if (r.get(r.size() - 1).getB() == 1) {
                    r.remove(r.size() - 1);
                    n1--;
                    newbal = n1 / (double) r.size();
                } else {
                    break;
                }
            }
        } else {
            while (newbal < bal) {
                if (r.get(0).getB() == 0) {
                    r.remove(0);
                    newbal = n1 / (double) r.size();
                } else {
                    break;
                }
            }
        }
        System.out.println("New balance=" + newbal);
    }

    /**
     *
     * @param annotatedFile
     * @param dataFile
     * @param outputdir
     * @param p
     * @param maxit
     * @param C
     * @throws IOException
     */
    public void cotraining(File annotatedFile, File dataFile, String outputdir, int p, int maxit, double C) throws IOException {
        UDPParser parser = new UDPParser(Config.getInstance().getValue("udp.address"), Config.getInstance().getValue("udp.model"));
        WikiExtractor ie = new WikiITSimpleDepExtractor();
        List<FileInstance> unlabelled = loadUnlabelled(dataFile);
        int it = 0;
        while (true) {
            // no enough unlabelled data
            if (unlabelled.size() < p) {
                break;
            }
            // shuffle unlabelled
            Collections.shuffle(unlabelled);
            // select p unlabelled data
            List<FileInstance> subList = unlabelled.subList(0, p);
            // build training
            TrainingSet ts = generateFeatures(annotatedFile, parser, ie);
            Utils.saveDict(ts.getDict(), new File(outputdir + "/tr_" + it + ".dict"));
            double bal = computeBalance(ts);
            System.out.println("Balance=" + bal);
            LOG.info("Training...");
            // build model
            Model model = train(ts, C);
            LOG.info("Testing...");
            // classify unlabeled data
            List<Pair<Integer, Integer>> r;
            if (thPred > 0) {
                r = testWithProb(model, generateTestFeatures(ts.getDict(), subList, parser, ie));
            } else {
                r = testWithoutProb(model, generateTestFeatures(ts.getDict(), subList, parser, ie));
            }
            balance(r, bal);
            LOG.log(Level.INFO, "New examples {0}", r.size());
            // save new training set
            File newFile = new File(outputdir + "/tr_" + it);
            Utils.copyByLine(annotatedFile, newFile);
            FileWriter writer = new FileWriter(newFile, true);
            CSVPrinter printer = CSVFormat.DEFAULT.print(writer);
            for (Pair<Integer, Integer> o : r) {
                FileInstance fi = unlabelled.get(o.getA());
                printer.printRecord("no title", fi.getText(), fi.getScore(), fi.getSubject(), fi.getPredicate(), fi.getObject(), o.getB().toString());
            }
            writer.close();
            annotatedFile = newFile;
            // remove
            Set<Integer> removeId = new HashSet<>();
            for (Pair<Integer, Integer> o : r) {
                removeId.add(o.getA());
            }
            for (int k = unlabelled.size() - 1; k >= 0; k--) {
                if (removeId.contains(unlabelled.get(k).getId())) {
                    unlabelled.remove(k);
                }
            }
            if (r.isEmpty()) {
                System.out.println("Interrupted...no new instances.");
                break;
            }
            it++;
            if (it >= maxit) {
                System.out.println("Interrupted...max iterations.");
                break;
            }
        }
    }

    /**
     *
     * @param annotatedFile
     * @param k
     * @param C
     * @throws IOException
     */
    @Deprecated
    public void kfold(File annotatedFile, int k, double C) throws IOException {
        UDPParser parser = new UDPParser(Config.getInstance().getValue("udp.address"), Config.getInstance().getValue("udp.model"));
        WikiExtractor ie = new WikiITSimpleDepExtractor();
        TrainingSet tr = generateFeatures(annotatedFile, parser, ie);
        LOG.info("Training (k-fold)...");
        // kfold
        kfold(tr, k, C);
    }

    /**
     *
     * @param trainFile
     * @param testFile
     * @param C
     * @throws IOException
     */
    public void trainAndTest(File trainFile, File testFile, double C) throws IOException {
        UDPParser parser = new UDPParser(Config.getInstance().getValue("udp.address"), Config.getInstance().getValue("udp.model"));
        WikiExtractor ie = new WikiITSimpleDepExtractor();
        TrainingSet tr = generateFeatures(trainFile, parser, ie);
        Model model = train(tr, C);
        List<FileInstance> unlabelled = loadUnlabelledCSV(testFile);
        List<Integer> labels = loadLabels(testFile);
        TrainingSet ts = generateTestFeatures(tr.getDict(), unlabelled, parser, ie);
        List<Integer> pred = test(model, ts);
        computeMetrics(labels, pred);
    }

    private double F(double P, double R) {
        return (P + R) == 0 ? 0 : 2 * P * R / (P + R);
    }

    /**
     *
     * @param labels
     * @param predicted
     */
    public void computeMetrics(List<Integer> labels, List<Integer> predicted) {
        int[][] m = new int[2][2];
        for (int i = 0; i < labels.size(); i++) {
            m[labels.get(i)][predicted.get(i)]++;
        }
        System.out.println("\tPred.");
        System.out.println("     *--------*--------*");
        System.out.printf("     |%8d|%8d|%n", m[0][0], m[0][1]);
        System.out.println("Lab. *--------*--------*");
        System.out.printf("     |%8d|%8d|%n", m[1][0], m[1][1]);
        System.out.println("     *--------*--------*");
        double p0 = (m[0][0] + m[1][0]) == 0 ? 0 : (double) m[0][0] / (double) (m[0][0] + m[1][0]);
        double r0 = (m[0][0] + m[0][1]) == 0 ? 0 : (double) m[0][0] / (double) (m[0][0] + m[0][1]);
        double p1 = (m[0][1] + m[1][1]) == 0 ? 0 : (double) m[1][1] / (double) (m[0][1] + m[1][1]);
        double r1 = (m[1][0] + m[1][1]) == 0 ? 0 : (double) m[1][1] / (double) (m[1][0] + m[1][1]);
        System.out.println("P_0=" + p0);
        System.out.println("R_0=" + r0);
        System.out.println("P_1=" + p1);
        System.out.println("R_1=" + r1);
        System.out.println("F_0=" + F(p0, r0));
        System.out.println("F_1=" + F(p1, r1));
        System.out.println("F_M=" + F(p0, r0) / F(p1, r1));
        System.out.println("acc.=" + ((double) (m[0][0] + m[1][1]) / (double) (m[0][0] + m[0][1] + m[1][0] + m[1][1])));
    }
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CoTraining ct = new CoTraining();
            // set the learning algorithm
            ct.setSolver(SolverType.L2R_LR);
            // set the threshold used during self-training
            ct.setThPred(0.85);
            // start co-training. Paramters: annotated data, unlabelled data, unlabelled data added to each iteration, max iterations, C
            ct.cotraining(new File("resources/bootstrapping/bootstrapping_train.csv"),
                    new File("resources/bootstrapping/triple_simpledep_text_20_01_dd.tsv"),
                    "resources/bootstrapping/new_reg/",
                    1000, 20, 10);
            // evaluate the training set obtained by the self-training
            ct.trainAndTest(new File("resources/bootstrapping/new_reg/tr_19"), new File("resources/bootstrapping/bootstrapping_test.csv"), 10);
        } catch (IOException ex) {
            Logger.getLogger(CoTraining.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     *
     * @return
     */
    public double getThPred() {
        return thPred;
    }

    /**
     *
     * @param thPred
     */
    public void setThPred(double thPred) {
        this.thPred = thPred;
    }

    /**
     *
     * @return
     */
    public SolverType getSolver() {
        return solver;
    }

    /**
     *
     * @param solver
     */
    public void setSolver(SolverType solver) {
        this.solver = solver;
    }

}
