import org.deeplearning4j.models.embeddings.learning.impl.elements.CBOW;
import org.deeplearning4j.models.embeddings.learning.impl.elements.SkipGram;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.nd4j.common.io.ClassPathResource;

import java.util.Collection;

public class Word2vec {

    public static void main(String[] args) throws Exception {

        String filePath = new ClassPathResource("billboard_lyrics_1964-2015.csv").getFile().getAbsolutePath();
        SentenceIterator iter = new BasicLineIterator(filePath);

        // CBOW
        Word2Vec vec = new Word2Vec.Builder()
                .layerSize(100)
                .windowSize(5)
                .iterate(iter)
                // .elementsLearningAlgorithm(new CBOW<>())
                // Skip-gram
                .elementsLearningAlgorithm(new SkipGram<>())
                .build();
        vec.fit();


        String[] words = new String[]{"guitar", "love", "rock"};
        for (String w : words) {
            Collection<String> lst = vec.wordsNearest(w, 2);
            System.out.println("2 Words closest to '" + w + "': " + lst);
        }
        System.out.println();

        String tw = "nice";
        Collection<String> wordsNearest = vec.wordsNearest(tw, 3);
        System.out.println(tw + " -> " + wordsNearest);
        for (String wn : wordsNearest) {
            double similarity = vec.similarity(tw, wn);
            System.out.println("sim(" + tw + "," + wn + ") : " + similarity);
        }
    }
}
