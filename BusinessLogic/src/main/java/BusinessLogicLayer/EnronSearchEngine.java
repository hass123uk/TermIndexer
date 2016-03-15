package BusinessLogicLayer;


import Database.ContainsRepository;
import Database.DocumentsRepository;
import Database.TermsRepository;
import FileSystem.FileLoader;
import FileSystem.FileLoaderImpl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static Database.Database.checkForTables;
import static Database.Database.initDatabase;

/**
 *
 */
public class EnronSearchEngine {

    private static final String HOME_DIR = "~";
    private static final String FILE_NAME = "/EnronDataSet";

    private static final String ALL_DOCS = "/MailDir_FullSet";
    private static final String HALF_ALL_DOCS = "/MailDir_HalfSet";
    private static final String FEW_DOCS = "/MailDir_SubSet";

    private static final int DEFAULT_MAX_THREADS = 10;

    private static final String ENRON_DATASET_DIR
            = HOME_DIR
            + FILE_NAME
            + ALL_DOCS;

    private static FileLoader fileLoader;
    private static StringSplitter splitter;
    private static ExecutorService pool;

    private static TermsRepository termsRepository;
    private static DocumentsRepository documentsRepository;
    private static ContainsRepository containsRepository;
    private static SynchronizedTermsMap synchronizedTermsMap;
    private static IncrementalIDGenerator incrementalIDGenerator;

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        fileLoader = new FileLoaderImpl();
        splitter = new StringSplitter("\\W+");

        if (!checkForTables()) {
            initDatabase();
        }
        incrementalIDGenerator = new IncrementalIDGenerator();
        createRepositories();
        pool = Executors.newWorkStealingPool(DEFAULT_MAX_THREADS);
        synchronizedTermsMap = new SynchronizedTermsMap(termsRepository.selectAllTerms());
        List<Callable<Void>> callables = loadFilesFromFSAndMapToCallables();
        invokeAll(callables);

        final long endTime = System.currentTimeMillis();
        System.out.print("Total execution time: " + TimeUnit.MILLISECONDS.toMinutes(endTime - startTime)
                + " minutes for " + callables.size() + " files.\n");
        shutdownAndAwaitTermination(pool);
    }

    private static void createRepositories() {
        termsRepository = new TermsRepository();
        documentsRepository = new DocumentsRepository();
        containsRepository = new ContainsRepository();
    }

    private static List<Callable<Void>> loadFilesFromFSAndMapToCallables() {
        return fileLoader
                .loadFiles(Paths.get(ENRON_DATASET_DIR.replaceFirst("^~", System.getProperty("user.home"))))
                .stream()
                .map(file -> newIndexFileTaskCallable(file.toPath()))
                .collect(Collectors.toList());
    }

    private static Callable<Void> newIndexFileTaskCallable(Path filePath) {
        return new IndexTaskCallable(filePath, incrementalIDGenerator,
                synchronizedTermsMap, fileLoader, splitter, documentsRepository, termsRepository, containsRepository);
    }

    private static List<Void> invokeAll(List<Callable<Void>> indexingTasks) {
        try {
            return pool.invokeAll(indexingTasks)
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }).collect(Collectors.toList());

        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
