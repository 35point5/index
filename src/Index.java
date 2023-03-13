import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class Index {
    private static final int K = 10000;
    Map<String,List<IndexEntry>> indexes,revIndexes;
    Set<String> coverNodes;
    void Init(List<Edge> l){
        coverNodes=new HashSet<String>();

        GenerateCover(l);
        indexes=new HashMap<String,List<IndexEntry>>();
        GenerateIndexes(l,indexes);
        revIndexes=new HashMap<String,List<IndexEntry>>();
        List<Edge> revEdges=new LinkedList<Edge>();
        for(Edge e:l){
            revEdges.add(new Edge(e.v,e.u,e.prob));
        }
        GenerateIndexes(revEdges,revIndexes);
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
        for (Edge e:l){
            index.computeIfAbsent(e.u, k -> new LinkedList<IndexEntry>());
            index.computeIfAbsent(e.v, k -> new LinkedList<IndexEntry>());
        }
        Map<String, Set<Edge>> mp=new HashMap<String, Set<Edge>>();
        for(Edge e:l){
            Set<Edge> st = mp.computeIfAbsent(e.u, k -> new HashSet<Edge>());
            st.add(e);
            mp.computeIfAbsent(e.v, k -> new HashSet<Edge>());
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
                            index.get(node).add(new IndexEntry(out.v, dis.get(out.v),0));
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
                                counter.merge(out.v, 1, Integer::sum);
                                visited.add(out.v);
                                q.offer(out.v);
                            }
                        }
                    }
                }
            }
            for(IndexEntry entry:index.get(node)){
                if (!entry.v.equals(node) && counter.containsKey(entry.v)){
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
    int QueryDis(String u,String v){
        if (coverNodes.contains(u) && coverNodes.contains(v)){
            for(IndexEntry out:indexes.get(u)){
                if (out.v.equals(v)){
                    return out.dis;
                }
            }
        } else if (!coverNodes.contains(u) && coverNodes.contains(v)) {
            for(IndexEntry cv:indexes.get(u)){
                for(IndexEntry out:indexes.get(cv.v)){
                    if (out.v.equals(v)){
                        return cv.dis+out.dis;
                    }
                }
            }
        } else if (coverNodes.contains(u) && !coverNodes.contains(v)) {
            for(IndexEntry cv:revIndexes.get(v)){
                for(IndexEntry out:revIndexes.get(cv.v)){
                    if (out.v.equals(u)){
                        return cv.dis+out.dis;
                    }
                }
            }
        } else if (!coverNodes.contains(u) && !coverNodes.contains(v)) {
            for(IndexEntry cvu:indexes.get(u)){
                for(IndexEntry cvv:revIndexes.get(v)){
                    for(IndexEntry out:indexes.get(cvu.v)){
                        if (out.v.equals(cvv.v)){
                            return cvu.dis+cvv.dis+out.dis;
                        }
                    }
                }
            }
        }
        return -1;
    }
    public static void main(String[] args) throws Exception {
        File f=new File("in.txt");
        Scanner s=new Scanner(f);
        List<Edge> l=new LinkedList<Edge>();
        while (true){
            try {
                int u,v;
                double p;
                u=s.nextInt();
                v=s.nextInt();
                p=s.nextDouble();
                Edge e=new Edge(Integer.toString(u),Integer.toString(v),p);
                l.add(e);
            }
            catch (NoSuchElementException e){
                break;
            }
        }
        Index x=new Index();
        x.Init(l);
        System.out.println(x.QueryDis("1","6"));
    }
}
