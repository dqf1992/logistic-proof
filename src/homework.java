import java.io.*;
import java.util.*;

public class homework {

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        String fileToRead = "input.txt";
        String fileToWrite = "output.txt";

        Scanner scan = new Scanner(new BufferedReader(new FileReader(fileToRead)));
        int NQ = Integer.parseInt(scan.nextLine().trim());
        String[] queries = new String[NQ];
        for(int i=0; i < NQ; i++) {
            queries[i] = scan.nextLine().replaceAll("\\s+","");
        }
        int NS = Integer.parseInt(scan.nextLine().trim());
        String[] sentences = new String[NS];
        for(int j=0; j < NS; j++) {
            sentences[j] = scan.nextLine().replaceAll("\\s+","");
        }
        scan.close();
        HashMap<String, Deque<String>> KB = (HashMap<String, Deque<String>>)buildKnowledgeBase(sentences);
        PrintWriter writer = new PrintWriter(fileToWrite, "UTF-8");
        for(String query: queries) {
            System.out.println(KB);
            if(proofByResolution(KBCopy(KB), query)) {
                writer.println("TRUE");
            } else {
                writer.println("FALSE");
            }
        }
        writer.close();
    }

    private static Map<String, Deque<String>> KBCopy(Map<String, Deque<String>> map) {
        Iterator it = map.entrySet().iterator();
        Map<String, Deque<String>> map2 = new HashMap<>();
        while (it.hasNext())   {
            Map.Entry pair = (Map.Entry)it.next();
            Deque<String> deque = new LinkedList<>();
            deque.addAll((Deque<String>)pair.getValue());
            map2.put((String)pair.getKey(), deque);
        }
        return map2;
    }

     private static Boolean proofByResolution(Map<String, Deque<String>> map, String query) {
         query = reverseLiteral(query);
         return !unification(map, query, new HashSet<>());
     }

     private static String reverseLiteral(String literal) {
        if(literal.charAt(0) == '~') {
            literal = literal.substring(1);
        } else {
            literal = '~' + literal;
        }
        return literal;
    }

     public static boolean unification(Map<String, Deque<String>> map, String query, Set<String> explored) {
         // can't find contradiction, then the query is true
         if (explored.contains(query)) return true;
         explored.add(query);
         String queryClause = query;
         String[] literals = queryClause.split("\\|");
         for (int i = 0; i < literals.length; i++) {
             String revLiteral = reverseLiteral(literals[i]);
             String key = getPredicatesFromClause(revLiteral)[0];
             if (!map.containsKey(key)) continue;
             Deque<String> oldClauses = map.get(key);
             Deque<String> newClauses = new LinkedList<>();
             for (String clause : oldClauses) {
                 // Found contradiction means the query is false
                 if (isContradiction(queryClause, clause)) {
                     System.out.println("Found contradiction: " + queryClause + " " + clause);
                     return false;
                 }
                 String newClause = unifyTwoClauses(queryClause, clause, key);
                 // Can't prove wrong, then the query is true, should look at other ways
                 if (newClause.equals("")) {
                     continue;
                 }
                 newClause = factorClause(newClause);
                 // Can't prove wrong, then the query is true. should look at other ways
                 if (explored.contains(newClause)) {
                     continue;
                 }
//                 System.out.println("unify " + queryClause + " and " + clause + " is " + newClause);
//                 Map<String, Deque<String>> newMap = KBCopy(map);
//                 newMap.get(key).add(newClause);
                 if (!unification(map, newClause, explored)) {
                     return false;
                 }
             }
         }
         // Can't find contradiction, then the query is true;
         return true;
     }

        //can't proof the reverse query wrong then the original query is false


    private static boolean isContradiction(String clause1, String clause2) {
        if(!isLiteral(clause2) || !isLiteral(clause1)) return false;
        if(!getPredicatesFromClause(clause1)[0].equals(reverseLiteral(getPredicatesFromClause(clause2)[0]))) {
           return false;
        }
        String[] para1s = getParametersFromLiteral(clause1);
        String[] para2s = getParametersFromLiteral(clause2);
        for(int i=0; i<para1s.length; i++) {
            if(!para1s[i].equals(para2s[i]) && para1s[i].length() != 1 && para2s[i].length() != 1) {
                return false;
            }
        }
        return true;
    }

    private static String unifyTwoClauses(String clause1, String clause2, String predicate) {
//        System.out.println("Before: " + clause1 + " " + clause2);
        String[] clauses = standardizeClauses(clause1, clause2);
//        System.out.println("After: " + clauses[0] + " " + clauses[1]);
        String[] literal1s = clauses[0].split("\\|");
        String[] literal2s = clauses[1].split("\\|");
        StringBuilder sb = new StringBuilder();
        Map<String, String> subsDict = new HashMap<>();
        String literal1 = null;
        String literal2 = null;
        for(String literal: literal1s) {
            if(literal.contains(reverseLiteral(predicate))) {
                literal1 = literal;
            }
        }
        for(String literal: literal2s) {
            if(literal.contains(predicate)) {
                literal2 = literal;
            }
        }
        String[] para1s = getParametersFromLiteral(literal1);
        String[] para2s = getParametersFromLiteral(literal2);
        for (int i = 0; i < para1s.length; i++) {
            if (!para1s[i].equals(para2s[i])) {
                if (para1s[i].length() == 1 || para2s[i].length() == 1) {
                    if (para1s[i].length() == 1) {
                        subsDict.put(para1s[i], para2s[i]);
                    } else {
                        subsDict.put(para2s[i], para1s[i]);
                    }
                } else {
                    return "";
                }
            }
        }

//        System.out.println(substitutions);
        for (String literal: literal1s) {
            if (literal.equals(literal1)) continue;
            String[] paras = getParametersFromLiteral(literal);
            for (int i=0; i<paras.length; i++) {
                if(subsDict.containsKey(paras[i])) {
                    paras[i] = subsDict.get(paras[i]);
                }
            }
            if(sb.length() != 0) sb.append('|');
            sb.append(getPredicatesFromClause(literal)[0]);
            sb.append('('+ String.join(",",paras) + ')');
        }

        for (String literal: literal2s) {
            if (literal.equals(literal2)) continue;
            String[] paras = getParametersFromLiteral(literal);
            for (int i=0; i<paras.length; i++) {
                if(subsDict.containsKey(paras[i])) {
                    paras[i] = subsDict.get(paras[i]);
                }
            }
            if(sb.length() != 0) sb.append('|');
            sb.append(getPredicatesFromClause(literal)[0]);
            sb.append('('+ String.join(",",paras) + ')');
        }
        return sb.toString();
    }

    private static String[] standardizeClauses(String clause1, String clause2) {
        String[] clauses = new String[2];
        int[] letters = new int[26];
        String[] literal1s = clause1.split("\\|");
        String[] literal2s = clause2.split("\\|");
        for(String literal:literal1s) {
            for(String para: getParametersFromLiteral(literal)) {
                if(para.length() == 1) {
                    letters[para.charAt(0)-'a'] = 1;
                }
            }
        }
        Map<Character, Character> subDict = new HashMap<>();
        for (String literal:literal2s) {
            for (String para: getParametersFromLiteral(literal)) {
                if ( para.length() == 1 && letters[para.charAt(0)-'a'] == 1) {
                    char c = para.charAt(0);
                    while (letters[c-'a'] > 0) {
                        c++;
                        if(c - 'a' >= 26) {
                            c-= 26;
                        }
                    }
                    if(c != para.charAt(0)) {
                        letters[para.charAt(0) - 'a'] = 2;
                        subDict.put(para.charAt(0), c);
                        letters[c - 'a'] = 1;
                    }
                }
            }
        }
        clauses[0] = clause1;

        if (subDict.isEmpty()) {
            clauses[1] = clause2;
            return clauses;
        }
        for (int j=0; j<literal2s.length; j++) {
            String[] paras = getParametersFromLiteral(literal2s[j]);
            for (int i=0; i<paras.length; i++) {
                if (paras[i].length() == 1 && subDict.containsKey(paras[i].charAt(0))) {
                    paras[i] = Character.toString(subDict.get(paras[i].charAt(0)));
                }
            }
            literal2s[j] = getPredicatesFromClause(literal2s[j])[0] + '(' + String.join(",",paras) + ')' ;
        }
        clauses[1] = String.join("|", literal2s);
        return clauses;
    }

    private static String[] getParametersFromLiteral(String literal) {
        int from = literal.indexOf('(');
        int to = literal.indexOf(')');
        return literal.substring(from+1, to).split(",");
    }

    private static boolean isLiteral(String str) {
        return str.split("\\|").length == 1;
    }

    private static Map<String, Deque<String>> buildKnowledgeBase(String[] sentences) {
        Map<String, Deque<String>> map = new HashMap<>();
        for (String sentence : sentences) {
            HashSet<String> clauseSet = parseSentenceToClauses(sentence, "");
            Deque<String> deque = new LinkedList<>(clauseSet);
            addToKnowledgeBase(map, deque);
        }
        return map;
    }

    private static void addToKnowledgeBase(Map<String,Deque<String>> map, Deque<String> clauses) {
        for (String clause : clauses) {
            for (String predicate : getPredicatesFromClause(clause)) {
                String key = predicate;
                if (!map.containsKey(key)) {
                    map.put(key, new LinkedList<String>());
                }
                Deque<String> clauses2 = map.get(key);
                clauses2.addLast(clause);
            }
        }
    }

    public static String[] getPredicatesFromClause(String clause) {
        String[] literals = clause.split("\\|");
        for(int i=0; i<literals.length; i++) {
            literals[i] = literals[i].substring(0, literals[i].indexOf('('));
        }
        return literals;
    }

    public static String[] getPredicateWordsFromClause(String clause) {
        String[] literals = clause.split("\\|");
        for(int i=0; i<literals.length; i++) {
            int from = 0;
            if(literals[i].charAt(0) == '~') from = 1;
            literals[i] = literals[i].substring(from, literals[i].indexOf('('));
        }
        return literals;
    }



    public static HashSet<String> parseSentenceToClauses(String str, String prefix) {
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
        } else if(isClauseA || isClauseB) {
            set.addAll( parseSentenceToClauses(str.substring(0, idx), prefix));
            set.addAll( parseSentenceToClauses(str.substring(idx+1, str.length()), prefix));
        } else {
            HashSet<String> cnfSet1 = parseSentenceToClauses(str.substring(0, idx), prefix);
            HashSet<String> cnfSet2 = parseSentenceToClauses(str.substring(idx+1, str.length()), prefix);
            for(String cnf1:cnfSet1) {
                for(String cnf2:cnfSet2) {
                    set.add(cnf1 + '|' + cnf2);
                }
            }
        }
        return set;
    }

    public static String parseLiteral(String str, String prefix) {
        assert (str.indexOf(')') == str.length() - 1);
        assert (str.indexOf('(') != -1);
        return prefix + str;
    }

    public static String factorClause(String clause) {
        String[] predicates = getPredicatesFromClause(clause);

        String[] literals = clause.split("\\|");

        for(int i=0; i<predicates.length; i++) {
            for(int j=i+1; j<predicates.length; j++) {
                if(predicates[i].equals(predicates[j]) && !predicates[i].equals("")) {
                    Boolean didUnify = true;
                    String[] para1s = getParametersFromLiteral(literals[i]);
                    String[] para2s = getParametersFromLiteral(literals[j]);
                    for (int k = 0; k < para1s.length; k++) {
                        if (!para1s[k].equals(para2s[k])) {
                            if (para1s[k].length() == 1 || para2s[k].length() == 1) {
                                if (para1s[k].length() == 1) {
                                    para1s[k] = para2s[k];
                                }
                            } else {
                                didUnify = false;
                                break;
                            }
                        }
                    }
                    if(didUnify) {
                        literals[i] = predicates[i] + "(" + String.join(",", para1s) + ")";
                        literals[j] = "";
                        predicates[j] = "";
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for(String literal: literals) {
            if(sb.length() != 0 && !literal.equals("")) {
                sb.append("|");
            }
            sb.append(literal);
        }
        return sb.toString();
    }

    public static int nextLevelOperatorIdx(String str) {
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
//                System.out.println("Next level Operator is " + str.charAt(i+1));
                return i + 1;
            }
        }
        return -1;
    }
}
