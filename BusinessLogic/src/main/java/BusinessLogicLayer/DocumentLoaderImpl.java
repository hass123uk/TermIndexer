package BusinessLogicLayer;

import DataAccessLayer.Database.PopulateDB;
import DataAccessLayer.FileSystem.FileLoader;
import com.enron.search.domainmodels.Document;
import com.enron.search.domainmodels.Term;
import java.io.File;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class DocumentLoaderImpl implements DocumentLoader {

    private final FileLoader fileLoader;
    private final TermSplitter termSplitter;
    private final PopulateDB populateDB;

    public DocumentLoaderImpl(FileLoader fileLoader, TermSplitter termSplitter,
            PopulateDB populateDB) {
        this.fileLoader = fileLoader;
        this.termSplitter = termSplitter;
        this.populateDB = populateDB;
    }

    public void saveDocuments(Path basePath) {
        long startTime = System.nanoTime();
        List<File> files = fileLoader.loadFiles(basePath);
        double executionTimeInSeconds = (System.nanoTime() - startTime) / 1E9;

        System.out.println(
                "Load Files: NumberOfFiles: " + files.size() + ".\n"
                + "ExecutionTime: " + executionTimeInSeconds);

        files.stream().forEach(new Consumer<File>() {
            @Override
            public void accept(File file) {
                long startLoadLines = System.nanoTime();
                List<String> lines = fileLoader.loadLines(file.toPath());
                List<Term> terms = termSplitter.splitLines(lines);
                double loadLinesExecutionTime = (System.nanoTime() - startLoadLines) / 1E9;

                Document document = new Document(
                        file.getAbsolutePath(),
                        new Date(System.currentTimeMillis()));

                long startSaveToDB = System.nanoTime();
                int docId = populateDB.saveDocument(document);
                terms.stream()
                        .forEach((Term term) -> {
                            int termId = populateDB.saveTerm(term.getTerm_Value(), docId);
                            populateDB.saveIndexInContainTbl(termId, docId);
                        });
                double saveToDBExecutionTime = (System.nanoTime() - startSaveToDB) / 1E9;

                System.out.println(
                        "Document: " + document.getDocument_URL()
                        + "\nLoad and Split Terms -"
                        + " ExecutionTime: " + loadLinesExecutionTime
                        + " NumberOfLines:" + terms.size()
                        + "\nSaveDocument - ExectionTime:" + saveToDBExecutionTime
                        + "\n"
                );
            }
        });
    }

    @Override
    public List<Document> loadDocuments(Path basePath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
