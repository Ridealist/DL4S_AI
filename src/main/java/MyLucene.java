import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MyLucene {

    public static void main(String[] args) throws Exception {
        // Directory: list of files where the inverted indexes are persisted
        Path path = Paths.get("/Users/ridealist/Desktop/dl4s_lucene/index/exercise");
        Directory directory = FSDirectory.open(path);

        // which fields to put into your documents and how their (index-time) text analysis pipelines should look
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();

        CharArraySet stopWords = new CharArraySet(Arrays.asList("a", "an", "the"), true);

        perFieldAnalyzers.put("pages", new StopAnalyzer(stopWords));
        perFieldAnalyzers.put("title", new WhitespaceAnalyzer());

        Analyzer analyzer = new PerFieldAnalyzerWrapper(new EnglishAnalyzer(), perFieldAnalyzers);

        // The inverted indexes are written on disk in a Directory
        // by an IndexWriter that will persist Documents according to an IndexWriterConfig
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(directory, config);

        Document dl4s = new Document();
        dl4s.add(new TextField("title", "Deep Learning for search", Field.Store.YES));
        dl4s.add(new TextField("page", "Living in the information age ...", Field.Store.YES));

        Document rs = new Document();
        rs.add(new TextField("title", "Relevant search", Field.Store.YES));
        rs.add(new TextField("page", "Getting a search engine to behave ...", Field.Store.YES));

        writer.addDocument(dl4s);
        writer.addDocument(rs);

        // you can persist them on the filesystem by issuing a commit.
        writer.commit();
        writer.close();

        // reading an inverted index by IndexReader
        IndexReader reader = DirectoryReader.open(directory);
        // By identifier, get Document form IndexReader directly
        // int identifier = 123;
        // Document document = reader.document(identifier);

        // make query for user input
        QueryParser parser = new QueryParser("title", new WhitespaceAnalyzer());
        Query query = parser.parse("+Deep +search");

        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs hits = searcher.search(query, 10);

        for (int i = 0; i < hits.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = hits.scoreDocs[i];

            Document doc = reader.document(scoreDoc.doc);

            System.out.println(doc.get("title") + " : " + scoreDoc.score);
        }
    }
}
