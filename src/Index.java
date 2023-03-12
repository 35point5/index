import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Index {
    private static final int K = 10000;
    Map<String,List<IndexEntry>> indexes,revIndexes;
    Set<String> coverNodes;
    void Init(List<Edge> l){
        coverNodes=new HashSet<String>();

        GenerateCover(l);
        GenerateIndexes(l,indexes);
    }
    void GenerateCover(List<Edge> l){
        for(Edge e:l){
            if (!coverNodes.contains(e.u) && !coverNodes.contains(e.v)){
                coverNodes.add(e.u);
                coverNodes.add(e.v);
            }
        }
    }
    void GenerateIndexes(List<Edge> l, Map<String,List<IndexEntry>> index){
        index=new HashMap<String,List<IndexEntry>>();
        for (Edge e:l){
            index.computeIfAbsent(e.u, k -> new LinkedList<IndexEntry>());
            index.computeIfAbsent(e.v, k -> new LinkedList<IndexEntry>());
        }
        Map<String, Set<Edge>> mp=new HashMap<String, Set<Edge>>();
        for(Edge e:l){
            Set<Edge> st = mp.computeIfAbsent(e.u, k -> new HashSet<Edge>());
            st.add(e);
        }

        for(Map.Entry<String,List<IndexEntry>> entry:index.entrySet()){
            entry.getValue().add(new IndexEntry(entry.getKey(),0,1)); //自己到自己
        }

        Map<String,Integer> counter=new HashMap<String, Integer>();
        for(String node:coverNodes){
            Set<String> visited=new HashSet<String>();
            Queue<String> q=new LinkedList<String>();
            Map<String,Integer> dis=new HashMap<String, Integer>();
            q.offer(node);
            visited.add(node);
            dis.put(node,0);
            while (!q.isEmpty()){
                String now=q.poll();
                for(Edge out:mp.get(now)){
                    if (!visited.contains(out.v)){
                        visited.add(out.v);
                        q.offer(out.v);
                        dis.put(out.v,dis.get(now)+1);
                        if (coverNodes.contains(out.v)){
                            index.get(out.v).add(new IndexEntry(out.v, dis.get(out.v),0));
                        }
                    }
                }
            }
            for (int i=0;i<K;++i){
                visited.clear();
                q.clear();
                q.offer(node);
                visited.add(node);
                while (!q.isEmpty()){
                    String now=q.poll();
                    for(Edge out:mp.get(now)){
                        if (!visited.contains(out.v)){
                            double temp=Math.random();
                            if (temp<=out.prob){
                                counter.put(out.v,counter.get(out.v)+1);
                                visited.add(out.v);
                                q.offer(out.v);
                            }
                        }
                    }
                }
            }
            for(IndexEntry entry:index.get(node)){
                if (!entry.v.equals(node)){
                    entry.prob=(double)counter.get(entry.v)/K;
                }
            }
        }

        for(Map.Entry<String,List<IndexEntry>> entry:index.entrySet()){
            String node=entry.getKey();
            if (!coverNodes.contains(node)){
                for(Edge out:mp.get(node)){
                    if (coverNodes.contains(out.v)){
                        entry.getValue().add(new IndexEntry(out.v, 1, out.prob));
                    }
                }
            }
        }

    }
    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader("in.txt"));
//        Scanner scan=in;
        String str;
        while ((str = in.readLine()) != null) {
            System.out.println(str);
        }
        System.out.println(str);
    }
}
