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
         return !unification(map, query);
     }

     private static String reverseLiteral(String literal) {
        if(literal.charAt(0) == '~') {
            literal = literal.substring(1);
        } else {
            literal = '~' + literal;
        }
        return literal;
    }

     private static boolean unification(Map<String, Deque<String>> map, String query) {
         // can't find contradiction, then the query is true
         Set<ClauseString> explored = new HashSet<>();
         explored.add(new ClauseString(query));
         Deque<String> clausesToResolve = getLiteralsFromKB(map);
         clausesToResolve.addFirst(query);
         union(map, query);
         while (!clausesToResolve.isEmpty() && clausesToResolve.size() <= 5000) {
             String queryClause = clausesToResolve.pollFirst();
             String[] literals = queryClause.split("\\|");
             for (int i = 0; i < literals.length; i++) {
                 String revLiteral = reverseLiteral(literals[i]);
                 String key = getPredicateFromLiteral(revLiteral);
                 if (!map.containsKey(key)) continue;
                 Deque<String> oldClauses = map.get(key);
                 Deque<String> newClauses = new LinkedList<>();
                 for (String clause : oldClauses) {
                     // Found contradiction means the query is false
                     if (isContradiction(queryClause, clause)) {
                         System.out.println("Found contradiction: " + queryClause + " " + clause);
                         return false;
                     }
                     String[] literal2s = clause.split("\\|");
                     for (int j = 0; j < literal2s.length; j++) {
                         if (!getPredicateFromLiteral(literal2s[j]).equals(key)) continue;
                         String newClause;
                         newClause = binaryRefutation(queryClause, clause, i, j);
                         newClause = factoring(newClause);
                         ClauseString newClauseCS = new ClauseString(newClause);
                         if (newClause.equals("") || explored.contains(newClauseCS)) {
                             continue;
                         }
                         explored.add(newClauseCS);
//                         System.out.println("unify " + queryClause + " and " + clause + " is " + newClause);
                         newClauses.add(newClause);
                     }
                 }
//                 for(String newClause: newClauses) {
//                     if(newClause.split("\\|").length <= 2) {
//                         clausesToResolve.add(newClause);
//                     }
//                 }
                 clausesToResolve.addAll(newClauses);
                 addToKnowledgeBase(map, newClauses);
             }
         }
         // Can't find contradiction, then the query is true;
         return true;
     }

    private static Deque<String> getLiteralsFromKB(Map<String, Deque<String>> map) {
        Deque<String> literals = new LinkedList<>();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext())   {
            Map.Entry pair = (Map.Entry)it.next();
            Deque<String> deque = map.get(pair.getKey());
            for(String clause: deque) {
                if(isLiteral(clause)) {
                    literals.add(clause);
                }
            }
        }
        return literals;
    }

    private static boolean isContradiction(String clause1, String clause2) {
        if(!isLiteral(clause2) || !isLiteral(clause1)) return false;
        if(!getPredicateFromLiteral(clause1).equals(reverseLiteral(getPredicateFromLiteral(clause2)))) {
           return false;
        }
        return !unifyTwoLiterals(clause1, reverseLiteral(clause2), new HashMap<>()).equals("");
    }

    private static String binaryRefutation(String clause1, String clause2, int li, int lj) {
        String[] clauses = standardizeClauses(clause1, clause2);
        String[] literal1s = clauses[0].split("\\|");
        String[] literal2s = clauses[1].split("\\|");
        Map<String, String> dic = new HashMap<>();
        String literal1 = literal1s[li];
        String literal2 = literal2s[lj];
        if(unifyTwoLiterals(literal1, reverseLiteral(literal2), dic).equals("")) return "";
        String temp1 = substitution(clauses[0], li, dic);
        String temp2 = substitution(clauses[1], lj, dic);
        if(temp1.equals("")) return temp2;
        if(temp2.equals("")) return temp1;
        return temp1 + "|" + temp2;
    }

    private static boolean isVariable(String para) {
        return para.length() == 1 && Character.isLowerCase(para.charAt(0));
    }

    private static String[] standardizeClauses(String clause1, String clause2) {
        String[] clauses = new String[2];
        int[] letters = new int[26];
        String[] literal1s = clause1.split("\\|");
        String[] literal2s = clause2.split("\\|");
        for(String literal:literal1s) {
            for(String para: getParametersFromLiteral(literal)) {
                if(isVariable(para)) {
                    letters[para.charAt(0)-'a'] = 1;
                }
            }
        }
        Map<Character, Character> subDict = new HashMap<>();
        for (String literal:literal2s) {
            for (String para: getParametersFromLiteral(literal)) {
                if ( isVariable(para) && letters[para.charAt(0)-'a'] == 1) {
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
                if (isVariable(paras[i]) && subDict.containsKey(paras[i].charAt(0))) {
                    paras[i] = Character.toString(subDict.get(paras[i].charAt(0)));
                }
            }
            literal2s[j] = getPredicateFromLiteral(literal2s[j]) + '(' + String.join(",",paras) + ')' ;
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

    private static String appendLiteral(String clause, String literal) {
        if(clause.equals("")) return literal;
        return clause + "|" + literal;
    }

    private static Map<String, Deque<String>> buildKnowledgeBase(String[] sentences) {
        Map<String, Deque<String>> map = new HashMap<>();
        for (String sentence : sentences) {
            HashSet<String> clauseSet = parseSentenceToClauses(sentence, "");
            addToKnowledgeBase(map, new LinkedList<>(clauseSet));
        }
        return map;
    }

    private static void addToKnowledgeBase(Map<String, Deque<String>> map, Deque<String> clauses) {
        for (String clause : clauses) {
            union(map, clause);
        }
    }

    private static void union(Map<String, Deque<String>> map, String clause) {
        String[] literals = clause.split("\\|");
        for(String literal: literals) {
            String key = getPredicateFromLiteral(literal);
            if (!map.containsKey(key)) {
                map.put(key, new LinkedList<>());
            }
            Deque<String> clauses = map.get(key);
            clauses.add(clause);
        }
    }

    private static HashSet<String> parseSentenceToClauses(String str, String prefix) {
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
                for(String cnf2: cnfSet2) {
                    set.add(cnf1 + '|' + cnf2);
                }
            }
        }
        return set;
    }

    private static String parseLiteral(String str, String prefix) {
        assert (str.indexOf(')') == str.length() - 1);
        assert (str.indexOf('(') != -1);
        return prefix + str;
    }

    private static String factoring(String clause) {
        if (clause.equals("")) return clause;
        String[] literals = clause.split("\\|");
        HashMap<String, String> dic = new HashMap<>();
        for (int i = 0; i < literals.length; i++) {
            for (int j = i + 1; j < literals.length; j++) {
                if (getPredicateFromLiteral(literals[i]).equals(getPredicateFromLiteral(literals[j]))) {
                    String newLiteral = unifyTwoLiterals(literals[i], literals[j], dic);
                    if ( !newLiteral.equals("") ) {
                        return factoring(substitution(clause, j, dic));
                    }
                }
            }
        }
        return clause;
    }

    private static String substitution(String clause, int idx, Map<String, String> dic) {
        String[] literals = clause.split("\\|");
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<literals.length; i++) {
            if ( i == idx ) { continue; }
            String[] paras = getParametersFromLiteral(literals[i]);
            for (int j=0; j<paras.length; j++) {
                if (dic.containsKey(paras[j])) {
                    paras[j] = dic.get(paras[j]);
                }
            }
            if(sb.length() != 0) sb.append('|');
            sb.append(getPredicateFromLiteral(literals[i]));
            sb.append('('+ String.join(",",paras) + ')');
        }
        return sb.toString();
    }

    private static String unifyTwoLiterals(String literal1, String literal2, Map<String, String> dic) {
        if (!getPredicateFromLiteral(literal1).equals(getPredicateFromLiteral(literal2))) return "";
        String[] para1s = getParametersFromLiteral(literal1);
        String[] para2s = getParametersFromLiteral(literal2);
        while(!String.join(",",para1s).equals(String.join(",", para2s))) {
            for (int i = 0; i < para1s.length; i++) {
                if (!para1s[i].equals(para2s[i])) {
                    if (isVariable(para1s[i]) && isVariable(para2s[i])) {
                        continue;
                    } else if (isVariable(para1s[i])) {
                        if (!dic.containsKey(para1s[i])) {
                            dic.put(para1s[i], para2s[i]);
                        }
                    } else if (isVariable(para2s[i])) {
                        if (!dic.containsKey(para2s[i])) {
                            dic.put(para2s[i], para1s[i]);
                        }
                    } else {
                        return "";
                    }
                }
            }
            for (int i = 0; i < para1s.length; i++) {
                if (dic.containsKey(para1s[i])) {
                    para1s[i] = dic.get(para1s[i]);
                }
                if (dic.containsKey(para2s[i])) {
                    para2s[i] = dic.get(para2s[i]);
                }
                if (!para1s[i].equals(para2s[i]) && isVariable(para1s[i]) && isVariable(para2s[i])) {
                    dic.put(para1s[i], para2s[i]);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getPredicateFromLiteral(literal1));
        sb.append("(" + String.join(",", para1s) + ")");
        return sb.toString();
    }

    private static String getPredicateFromLiteral(String literal) {
        return literal.substring(0, literal.indexOf('('));
    }

    private static int nextLevelOperatorIdx(String str) {
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

class ClauseString {
    String str;

    public ClauseString(String str) {
        this.str = str;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClauseString)) {
            return false;
        }
        ClauseString that = (ClauseString) obj;
        String[] literal1s = this.str.split("\\|");
        Arrays.sort(literal1s);
        String[] literal2s = that.str.split("\\|");
        Arrays.sort(literal2s);
        if (literal1s.length != literal2s.length) return false;
        HashMap<Character,Character> dic = new HashMap<>();
        for (int i = 0; i < literal1s.length; i++) {
            if (!literal1s[i].equals(literal2s[i])) {
                if (getPredicateFromLiteral(literal1s[i]).equals(getPredicateFromLiteral(literal2s[i]))) {
                    String[] para1s = getParametersFromLiteral(literal1s[i]);
                    String[] para2s = getParametersFromLiteral(literal2s[i]);
                    for (int j = 0; j < para1s.length; j++) {
                        if (!para1s[j].equals(para2s[j])) {
                            if (!isVariable(para1s[j]) || !isVariable(para2s[j])) {
                                return false;
                            } else {
                                if(!dic.containsKey(para2s[j].charAt(0))) {
                                    dic.put(para2s[j].charAt(0), para1s[j].charAt(0));
                                }
                                para2s[j] = Character.toString(dic.get(para2s[j].charAt(0)));
                            }
                        }
                    }
                    if(!String.join(",", para1s).equals(String.join(",",para2s))) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        String[] literals = this.str.split("\\|");
        Arrays.sort(literals);
        List<String> anyStringList = new ArrayList<>();
        for(String literal: literals) {
            anyStringList.add(getPredicateFromLiteral(literal));
            String[] paras = getParametersFromLiteral(literal);
            for(String para: paras) {
                if(isVariable(para)) {
                    anyStringList.add("var");
                } else {
                    anyStringList.add(para);
                }
            }
        }
        return Arrays.hashCode(anyStringList.toArray());
    }


    private static boolean isVariable(String para) {
        return para.length() == 1 && Character.isLowerCase(para.charAt(0));
    }

    private static String[] getParametersFromLiteral(String literal) {
        int from = literal.indexOf('(');
        int to = literal.indexOf(')');
        return literal.substring(from+1, to).split(",");
    }

    private static String getPredicateFromLiteral(String literal) {
        return literal.substring(0, literal.indexOf('('));
    }
}
