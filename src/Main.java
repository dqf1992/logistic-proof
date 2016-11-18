import java.util.*;

public class Main {

    public static void main(String[] args) {
        HashMap<String, Set<String>> map = new HashMap<>();
        String str = "( ( ~(Parent(x,y) | Ancestor(y,z))) & Ancestor(x,z))";
        str = str.replaceAll("\\s+","");
        parseSentence(str,"",map);
        System.out.println(map);
    }

    static public void parseSentence(String str, String prefix, HashMap<String, Set<String>> map) {
        System.out.println(str);
        if(str.charAt(0) != '(') {
            parseSimpleSentence(str, prefix, map);
            return;
        }
        char[] charArr = str.toCharArray();
        if(charArr[1] == '~') {
            prefix = prefix.equals("")? "~": "";
            parseSentence(str.substring(2,charArr.length-1), prefix, map);
            return;
        } else if(charArr[1] != '(') {
            boolean isClauseA = (str.indexOf('&') != -1) && prefix.equals("");
            boolean isClauseB = (str.indexOf('|') != -1) && prefix.equals("~");
            boolean isImplication = str.indexOf('=') != -1;
            int idx = str.indexOf('&') == -1 ? str.indexOf('|'): str.indexOf('&');
            if(isImplication) {
                String newStr = "((~" + str.substring(1,str.indexOf('=')) + ")|" + str.substring(str.indexOf('>')+1);
                System.out.println("The new string is:" + newStr);
                parseSentence(newStr, prefix, map);
            } else if(isClauseA || isClauseB) {
                parseSimpleSentence(str.substring(1, idx), prefix, map);
                parseSimpleSentence(str.substring(idx + 1, charArr.length-1), prefix, map);
            } else {
                parseSimpleSentence(str.substring(1, idx), prefix, map, str.substring(1,charArr.length-1));
                parseSimpleSentence(str.substring(idx + 1, charArr.length - 1), prefix, map, str.substring(1, charArr.length-1));
            }
            return;
        }
        int cnt = 0;
        for(int i=1; i<charArr.length-1; i++) {
            if(charArr[i] != '(' && charArr[i] != ')') continue;
            if(charArr[i] == '(') {
                cnt ++;
            } else if(charArr[i] == ')') {
                cnt --;
            }
            if(cnt == 0) {
                assert(charArr[i+1] == '&' || charArr[i+1] == '|' || charArr[i+1] == '=');
                if(charArr[i+1] == '=') {
                    String newStr = "((~" + str.substring(1,str.indexOf('=')) + ")|" + str.substring(str.indexOf('>')+1);
                    System.out.println("The new string is:" + newStr);
                    parseSentence(newStr, prefix, map);
                } else {
                    parseSentence(str.substring(1, i + 1), prefix, map);
                    parseSentence(str.substring(i + 2, charArr.length - 1), prefix, map);
                }
                return;
            }
        }
    }

    static public void parseSimpleSentence(String str, String prefix, HashMap<String, Set<String>> map) {
        assert(str.indexOf(')') == str.length()-1);
        assert(str.indexOf('(') != -1);
        String predicate = prefix + str.substring(0, str.indexOf('('));
        if(!map.containsKey(predicate)) {
            map.put(predicate, new HashSet<String>());
        }
        Set<String> set = map.get(predicate);
        set.add(prefix + str);
        map.put(predicate, set);
    }

    static public void parseSimpleSentence(String str, String prefix, HashMap<String, Set<String>> map, String clause) {
        assert (str.indexOf(')') == str.length() - 1);
        assert (str.indexOf('(') != -1);
        String predicate = prefix + str.substring(0, str.indexOf('('));
        if (!map.containsKey(predicate)) {
            map.put(predicate, new HashSet<String>());
        }
        Set<String> set = map.get(predicate);
        set.add(prefix + '(' + clause + ')');
        map.put(predicate, set);
    }

}
