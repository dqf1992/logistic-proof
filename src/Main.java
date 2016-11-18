import java.util.*;

public class Main {

    public static void main(String[] args) {
        String str = "( ((~Parent(x,y)) & Ancestor(y,z)) | (Ancestor(z,x) & Ancestor(x,z)) )";
        str = str.replaceAll("\\s+","");
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> set = parseStringToCNF(str, "");
        System.out.println(set);
    }

    static public HashSet<String> parseStringToCNF(String str, String prefix) {
        HashSet<String> set = new HashSet<>();
        if(str.charAt(0) != '(') {
            set.add(parseLiteral(str, prefix));
            return set;
        }
        if(str.charAt(1) == '~') {
            prefix = prefix.equals("")? "~": "";
            set.add(parseLiteral(str.substring(2, str.length()-1), prefix));
            return set;
        }
        System.out.println(str);
//        if(str.charAt(1) != '(') {
//            int idx = nextLevelOperatorIdx(str);
//            boolean isClauseA = str.charAt(idx) == '&' && prefix.equals("");
//            boolean isClauseB = str.charAt(idx) == '|' && prefix.equals("~");
//            boolean isImplication = str.charAt(idx) == '=';
//            if (isImplication) {
//                String newStr = "((~" + str.substring(1, str.indexOf('=')) + ")|" + str.substring(str.indexOf('>') + 1);
////                System.out.println("The new string is:" + newStr);
//                set.addAll(parseStringToCNF(newStr, prefix));
//            } else if (isClauseA || isClauseB) {
//                set.add(parseLiteral(str.substring(1, idx), prefix));
//                set.addAll(parseStringToCNF(str.substring(idx + 1, str.length() - 1), prefix));
//            } else {
//                String literal = parseLiteral(str.substring(1, idx), prefix);
//                HashSet<String> cnfSet = parseStringToCNF(str.substring(idx + 1, str.length() - 1), prefix);
//                for (String cnf : cnfSet) {
//                    set.add(cnf + '|' + literal);
//                }
//            }
//            return set;
//        }
        // remove the outermost brackets
        str = str.substring(1,str.length()-1);
        int idx = nextLevelOperatorIdx(str);
        assert(str.charAt(idx) == '&' || str.charAt(idx) == '|' || str.charAt(idx) == '=');
        boolean isClauseA = str.charAt(idx) == '&' && prefix.equals("");
        boolean isClauseB = str.charAt(idx) == '|' && prefix.equals("~");
        boolean isImplication = str.charAt(idx) == '=';
        if(isImplication) {
            HashSet<String> cnfSet2 = parseStringToCNF(str.substring(idx+2, str.length()), prefix);
            prefix = prefix.equals("")? "~": "";
            HashSet<String> cnfSet1 = parseStringToCNF(str.substring(0,idx), prefix);
            for(String cnf1: cnfSet1) {
                for(String cnf2: cnfSet2) {
                    set.add(cnf1 + '|' + cnf2);
                }
            }
        } else if(str.charAt(idx) == '&') {
            set.addAll(parseStringToCNF(str.substring(0, idx), prefix));
            set.addAll(parseStringToCNF(str.substring(idx+1, str.length()), prefix));
        } else if(str.charAt(idx) == '|'){
            HashSet<String> cnfSet1 = parseStringToCNF(str.substring(0, idx), prefix);
            HashSet<String> cnfSet2 = parseStringToCNF(str.substring(idx+1, str.length()), prefix);
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
