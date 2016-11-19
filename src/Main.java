import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
            String fileToRead = "input.txt";
            String fileToWrite = "output.txt";
            if(args.length > 0) {
                fileToRead = args[0];
                if(args.length > 1) {
                    fileToWrite = args[1];
                }
            }
            Scanner scan = new Scanner(new BufferedReader(new FileReader(fileToRead)));
            int NQ = scan.nextInt();
            String[] queries = new String[NQ];
            for(int i=0; i < NQ; i++) {
                queries[i] = scan.nextLine().replaceAll("\\s+","");
            }
            int NS = scan.nextInt();
            String[] sentences = new String[NS];
            for(int j=0; j < NS; j++) {
                sentences[j] = scan.nextLine().replaceAll("\\s+","");
            }
            scan.close();
            Main m = new Main();
            HashMap<String, HashSet<String>> KB = m.buildKnowledgeBase(sentences);
            PrintWriter writer = new PrintWriter(fileToWrite, "UTF-8");
            for(String query: queries) {
                HashMap<String, HashSet<String>> KBCopy = (HashMap<String, HashSet<String>>) KB.clone();
                if(proofByResolution(KBCopy, query)) {
                    writer.println("TRUE");
                } else {
                    writer.println("FALSE");
                }
            }
            writer.close();
        }
    }

    static public Boolean proofByResolution(HashMap<String, HashSet<String>> map, String query) {
        String reverseQuery;
        if(query.charAt(0) == '~') {
            reverseQuery = query.substring(1);
        } else {
            reverseQuery = '~' + query;
        }
        HashSet<String> set = map.get(query);
        return true;
    }

    public HashMap<String, HashSet<String>> buildKnowledgeBase(String[] sentences) {
        HashMap<String, HashSet<String>> map = new HashMap<>();
        int i = 0;
        for(String sentence: sentences) {
            HashSet<String> clauseSet = parseSentenceToClauses(sentence, "");
            changeVariablesName(clauseSet, i++);
            for(String clause: clauseSet) {
                for (String predicate : getPredicatesFromClause(clause)) {
                    String key = predicate;
                    if (!map.containsKey(key)) {
                        map.put(key, new HashSet<String>());
                    }
                    HashSet<String> clauseSet2 = map.get(key);
                    clauseSet2.add(clause);
                    map.put(key, clauseSet2);
                }
            }
        }
        System.out.println(map);
        return map;
    }

    static void changeVariablesName(HashSet<String> set, int suffix) {
        Iterator<String> it = set.iterator();
        while(it.hasNext()) {
            int from = 0;
            StringBuilder sb = new StringBuilder(it.next());
            while(from<sb.length()) {
                int idx = sb.indexOf("(", from);
                if (idx != -1) {
                    break;
                }
                from = sb.indexOf(")");
                if (from - idx == 2) {
                    sb.insert(from, Integer.toString(suffix));
                }
            }
            it.remove();
        }
    }

    static public String[] getPredicatesFromClause(String clause) {
        String[] literals = clause.split("\\|");
        for(int i=0; i<literals.length; i++) {
            literals[i] = literals[i].substring(0, literals[i].indexOf('('));
        }
        return literals;
    }

    static public HashSet<String> parseSentenceToClauses(String str, String prefix) {
        HashSet<String> set = new HashSet<>();
        if(str.charAt(0) != '(') {
            set.add(parseLiteral(str, prefix));
            return set;
        }
        if(str.charAt(1) == '~') {
            prefix = prefix.equals("")? "~": "";
            set.addAll(> parseSentenceToClauses(str.substring(2, str.length()-1), prefix));
            return set;
        }
        System.out.println(str);
        // remove the outermost brackets
        str = str.substring(1,str.length()-1);
        int idx = nextLevelOperatorIdx(str);
        assert(str.charAt(idx) == '&' || str.charAt(idx) == '|' || str.charAt(idx) == '=');
        boolean isClauseA = str.charAt(idx) == '&' && prefix.equals("");
        boolean isClauseB = str.charAt(idx) == '|' && prefix.equals("~");
        boolean isImplication = str.charAt(idx) == '=';
        if(isImplication) {
            HashSet<String> cnfSet2 = > parseSentenceToClauses(str.substring(idx+2, str.length()), prefix);
            prefix = prefix.equals("")? "~": "";
            HashSet<String> cnfSet1 = > parseSentenceToClauses(str.substring(0,idx), prefix);
            for(String cnf1: cnfSet1) {
                for(String cnf2: cnfSet2) {
                    set.add(cnf1 + '|' + cnf2);
                }
            }
        } else if(str.charAt(idx) == '&') {
            set.addAll(> parseSentenceToClauses(str.substring(0, idx), prefix));
            set.addAll(> parseSentenceToClauses(str.substring(idx+1, str.length()), prefix));
        } else if(str.charAt(idx) == '|'){
            HashSet<String> cnfSet1 = > parseSentenceToClauses(str.substring(0, idx), prefix);
            HashSet<String> cnfSet2 = > parseSentenceToClauses(str.substring(idx+1, str.length()), prefix);
            for(String cnf1:cnfSet1) {
                for(String cnf2:cnfSet2) {
                    set.add(cnf1 + '|' + cnf2);
                }
            }
        } else {
            System.out.println("Illegal operator!!");
        }
        return set;
    }

    static public String parseLiteral(String str, String prefix) {
        assert (str.indexOf(')') == str.length() - 1);
        assert (str.indexOf('(') != -1);
        System.out.println(str);
        return prefix + str;
    }

    static public int nextLevelOperatorIdx(String str) {
        //do not include the outermost brackets
        int cnt = 0;
        for(int i=0; i<str.length(); i++) {
            if(str.charAt(i) != '(' && str.charAt(i) != ')') continue;
            if(str.charAt(i) == '(') {
                cnt ++;
            } else if(str.charAt(i) == ')') {
                cnt --;
            }
            if(cnt == 0) {
                System.out.println("Next level Operator is " + str.charAt(i+1));
                return i + 1;
            }
        }
        return -1;
    }
}
