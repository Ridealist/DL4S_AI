import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.CharsRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SynonymExpansion {

    public static CustomAnalyzer getCustomAnalyzer() throws IOException {
        Map<String, String> sffargs = new HashMap<>();
        sffargs.put("synonyms", "synonyms.txt");
        sffargs.put("ignoreCase", "true");
        // for https://wordnet.princeton.edu WordNet format Settings
//        sffargs.put("synonyms", "synonyms-wn.txt");
//        sffargs.put("format", "wordnet");

        CustomAnalyzer.Builder builder = CustomAnalyzer.builder()
                .withTokenizer(WhitespaceTokenizerFactory.class)
                .addTokenFilter(SynonymGraphFilterFactory.class, sffargs);

        return builder.build();
    }

    public static void main(String[] args) throws Exception {
        // build synonym map
        SynonymMap.Builder builder = new SynonymMap.Builder(true);
        builder.add(new CharsRef("aeroplane"), new CharsRef("plane"), true);
        builder.add(new CharsRef("aeroplane"), new CharsRef("aircraft"), true);
        final SynonymMap map = builder.build();

        // define analyzer
        Analyzer indexTimeAnalyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer tokenizer = new WhitespaceTokenizer();
                SynonymGraphFilter synFilter = new SynonymGraphFilter(tokenizer, map, true);
                return new TokenStreamComponents(tokenizer, synFilter);
            }
        };
        Analyzer searchTimeAnalyzer = new WhitespaceAnalyzer();

        // make integrated analyzer
        Directory directory = FSDirectory.open(Paths.get("/Users/ridealist/Desktop/dl4s_lucene/index/synonym"));

        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put("year", new KeywordAnalyzer());

        CustomAnalyzer customAnalyzer = getCustomAnalyzer();

        // Analyzer analyzer = new PerFieldAnalyzerWrapper(indexTimeAnalyzer, perFieldAnalyzers);
        Analyzer analyzer = new PerFieldAnalyzerWrapper(customAnalyzer, perFieldAnalyzers);

        // apply analyzer to IndexWriter
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(directory, config);

        // add docs to IndexWriter
        Document aeroplaneDoc = new Document();
        aeroplaneDoc.add(new TextField("title", "Aeroplane", Field.Store.YES));
        aeroplaneDoc.add(new TextField("author", "Red Hot Chili Peppers", Field.Store.YES));
        aeroplaneDoc.add(new TextField("year", "1995", Field.Store.YES));
        aeroplaneDoc.add(new TextField("album", "One Hot Minute", Field.Store.YES));
        aeroplaneDoc.add(new TextField("text",
                "I like pleasure spiked with pain and music is my aeroplane ...", Field.Store.YES));

        writer.addDocument(aeroplaneDoc);
        writer.commit();

        // make search
        IndexReader reader = DirectoryReader.open(directory);

        IndexSearcher searcher = new IndexSearcher(reader);

        QueryParser parser = new QueryParser("text", searchTimeAnalyzer);

        Query query = parser.parse("plane");

        TopDocs hits = searcher.search(query, 10);

        for (int i = 0; i < hits.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = hits.scoreDocs[i];
            Document doc = searcher.doc(scoreDoc.doc);
            System.out.println(doc.get("title") + " by " + doc.get("author"));
        }
    }
}
