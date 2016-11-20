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

     public static Boolean proofByResolution(HashMap<String, HashSet<String>> map, String query) {
        return unification(map, query);
    }

     public static String reverseLiteral(String literal) {
        if(literal.charAt(0) == '~') {
            literal = literal.substring(1);
        } else {
            literal = '~' + literal;
        }
        return literal;
    }

     public static boolean unification(HashMap<String, HashSet<String>> map, String query) {
        Deque<String> toResolveLiterals = new LinkedList<>();
        toResolveLiterals.addLast(reverseLiteral(query));
        while(!toResolveLiterals.isEmpty()) {
            String literal = toResolveLiterals.pollFirst();
            //find reverse literal to resolve
            HashSet<String> oldClauses = map.get(reverseLiteral(literal));
            HashSet<String> newClauses = new HashSet<>();
            for(String clause: oldClauses) {
                String newClause = unifyTwoClauses(literal, clause);
                if(isLiteral(newClause)) toResolveLiterals.add(newClause);
                newClauses.add(newClause);
            }
            addToKnowledgeBase(map, newClauses);
            removeFromKnowledgeBase(map, oldClauses);
        }
        return true;
    }

    private static String unifyTwoClauses(String literal, String clause) {
        String predicate = getPredicatesFromClause(literal)[0];
        String[] literals = clause.split("\\|");
        StringBuilder sb = new StringBuilder();
        Map<String, String> substitutions = new HashMap<>();
        for (String literal2 : literals) {
            String predicate2 = getPredicatesFromClause(literal2)[0];
            if (predicate2.equals(predicate)) {
                String[] paras = getParameters(literal);
                String[] para2s = getParameters(literal2);
                for (int i = 0; i < paras.length; i++) {
                    if (!paras[i].equals(para2s)) {
                        assert paras[i].length() == 1 || para2s[i].length() == 1 : "can't unify with two constant";
                        if (paras[i].length() == 1) {
                            substitutions.put(paras[i], para2s[i]);
                        }
                    }
                }
                break;
            }
        }
        for (String literal2: literals) {
            if(literal.equals(literal2)) continue;
            String[] paras = getParameters(literal2);
            for (int i=0; i<paras.length; i++) {
                if(substitutions.containsKey(paras[i])) {
                    paras[i] = substitutions.get(paras[i]);
                }
            }
            sb.append(getPredicatesFromClause(literal2)[0]);
            sb.append('('+ String.join(",",paras) + ')');
        }
        return sb.toString();
    }

    private static String[] getParameters(String literal) {
        int from = literal.indexOf('(');
        int to = literal.indexOf(')');
        return literal.substring(from+1, to).split("\\|");
    }

    public static boolean isLiteral(String str) {
        return !str.contains("\\|") && !str.contains("&");
    }

    public HashMap<String, HashSet<String>> buildKnowledgeBase(String[] sentences) {
        HashMap<String, HashSet<String>> map = new HashMap<>();
        for (String sentence : sentences) {
            HashSet<String> clauseSet = parseSentenceToClauses(sentence, "");
            addToKnowledgeBase(map, clauseSet);
        }
        System.out.println(map);
        return map;
    }

    static public void addToKnowledgeBase(HashMap<String,HashSet<String>> map, HashSet<String> clauseSet) {
        for (String clause : clauseSet) {
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

    static public void removeFromKnowledgeBase(HashMap<String, HashSet<String>> map, HashSet<String> oldClauses) {
        for(String oldClause: oldClauses) {
            for (String predicate : getPredicatesFromClause(oldClause)) {
                String key = predicate;
                if(map.containsKey(key)) {
                    HashSet<String> clauseSet = map.get(key);
                    clauseSet.remove(oldClause);
                    map.put(key, clauseSet);
                }
            }
        }
    }

    static public String[] getPredicatesFromClause(String clause) {
        String[] literals = clause.split("\\|");
        for(int i=0; i<literals.length; i++) {
            int from = 0;
            if(literals[i].charAt(0) == '~') from = 1;
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
            set.addAll(parseSentenceToClauses(str.substring(2, str.length()-1), prefix));
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
            HashSet<String> cnfSet2 = parseSentenceToClauses(str.substring(idx+2, str.length()), prefix);
            prefix = prefix.equals("")? "~": "";
            HashSet<String> cnfSet1 = parseSentenceToClauses(str.substring(0,idx), prefix);
            for(String cnf1: cnfSet1) {
                for(String cnf2: cnfSet2) {
                    set.add(cnf1 + '|' + cnf2);
                }
            }
        } else if(str.charAt(idx) == '&') {
            set.addAll( parseSentenceToClauses(str.substring(0, idx), prefix));
            set.addAll( parseSentenceToClauses(str.substring(idx+1, str.length()), prefix));
        } else if(str.charAt(idx) == '|'){
            HashSet<String> cnfSet1 = parseSentenceToClauses(str.substring(0, idx), prefix);
            HashSet<String> cnfSet2 = parseSentenceToClauses(str.substring(idx+1, str.length()), prefix);
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
