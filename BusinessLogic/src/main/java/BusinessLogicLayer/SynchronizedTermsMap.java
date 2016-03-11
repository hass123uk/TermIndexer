package BusinessLogicLayer;

import com.enron.search.domainmodels.Term;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by HassanMahmud on 05/03/2016.
 */
public class SynchronizedTermsMap {
    public static final String TERM_NOT_PRESENT = "TermNotPresent";
    public Lock lock = new ReentrantLock();
    private Map<String, String> syncTermsMap;

    public SynchronizedTermsMap(List<Term> terms) {
        Map<String, String> termsMap = terms.stream().collect(
                Collectors.toMap(Term::getTerm_Value, Term::getTerm_ID));
        syncTermsMap = Collections.synchronizedMap(termsMap);
    }

    public synchronized int getMapSize(){
        return syncTermsMap.size();
    }

    public synchronized String getTermIDIfPresent(String term_value) {
        boolean containsTermValue = syncTermsMap.containsKey(term_value);
        if (containsTermValue) {
            return syncTermsMap.get(term_value);
        } else {
            return TERM_NOT_PRESENT;
        }
    }

    public synchronized void putTerm(String term_value, String term_id){
        syncTermsMap.put(term_value, term_id);
    }


//    public synchronized String getIdOrGenerateNewId(String term_value) {
//        boolean containsTermValue = syncTermsMap.containsKey(term_value);
//        if (containsTermValue) {
//            return syncTermsMap.get(term_value);
//        } else {
//            String newId = incrementalIDGenerator.termIdGenerator();
//            syncTermsMap.put(term_value, newId);
//            return newId;
//        }
//    }
}
