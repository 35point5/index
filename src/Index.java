import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class Index {
    private static final int K = 100;
    Map<String, Map<String, IndexEntry>> indexes, revIndexes; //每个点的正反索引
    Set<String> coverNodes; //点覆盖集合

    void Init(List<Edge> l) { //初始化
        coverNodes = new HashSet<String>();

        GenerateCover(l); //生成点覆盖
        indexes = new HashMap<String, Map<String, IndexEntry>>();
        GenerateIndexes(l, indexes); //生成索引
        revIndexes = new HashMap<String, Map<String, IndexEntry>>();
        List<Edge> revEdges = new LinkedList<Edge>();
        for (Edge e : l) {
            revEdges.add(new Edge(e.v, e.u, e.prob)); //建反向图
        }
        GenerateIndexes(revEdges, revIndexes); //生成反向索引
    }

    void GenerateCover(List<Edge> l) {
        for (Edge e : l) {
            if (!coverNodes.contains(e.u) && !coverNodes.contains(e.v)) { //一条边的两个顶点都不在点覆盖中
                coverNodes.add(e.u); //加入点覆盖
                coverNodes.add(e.v);
            }
        }
    }

    void GenerateIndexes(List<Edge> l, Map<String, Map<String, IndexEntry>> index) {
        for (Edge e : l) {
            index.computeIfAbsent(e.u, k -> new HashMap<String, IndexEntry>()); //初始化
            index.computeIfAbsent(e.v, k -> new HashMap<String, IndexEntry>());
        }
        Map<String, Set<Edge>> mp = new HashMap<String, Set<Edge>>(); //每个点连出哪些边
        for (Edge e : l) {
            Set<Edge> st = mp.computeIfAbsent(e.u, k -> new HashSet<Edge>());
            st.add(e);
            mp.computeIfAbsent(e.v, k -> new HashSet<Edge>());
        }

        for (Map.Entry<String, Map<String, IndexEntry>> entry : index.entrySet()) {
            entry.getValue().put(entry.getKey(),new IndexEntry(entry.getKey(), 0, 1)); //自己到自己
        }

        Map<String, Integer> counter = new HashMap<String, Integer>();
        for (String node : coverNodes) { //枚举点覆盖中的点
            Set<String> visited = new HashSet<String>();
            Queue<String> q = new LinkedList<String>();
            Map<String, Integer> dis = new HashMap<String, Integer>();
            //BFS算最短路
            q.offer(node);
            visited.add(node);
            dis.put(node, 0);
            while (!q.isEmpty()) {
                String now = q.poll();
                for (Edge out : mp.get(now)) {
                    if (!visited.contains(out.v)) {
                        visited.add(out.v);
                        q.offer(out.v);
                        dis.put(out.v, dis.get(now) + 1);
                        if (coverNodes.contains(out.v)) { //只计算到其他点覆盖中的点
                            index.get(node).put(out.v, new IndexEntry(out.v, dis.get(out.v), 0));
                        }
                    }
                }
            }
            //采样K次算可达概率
            for (int i = 0; i < K; ++i) {
                visited.clear();
                q.clear();
                q.offer(node);
                visited.add(node);
                while (!q.isEmpty()) {
                    String now = q.poll();
                    for (Edge out : mp.get(now)) {
                        if (!visited.contains(out.v)) { //只算可达的点
                            double temp = Math.random();
                            if (temp <= out.prob) {
                                counter.merge(out.v, 1, Integer::sum);
                                visited.add(out.v);
                                q.offer(out.v);
                            }
                        }
                    }
                }
            }
            //计算可达概率
            for (Map.Entry<String, IndexEntry> entry : index.get(node).entrySet()) {
                if (!entry.getKey().equals(node) && counter.containsKey(entry.getKey())) {
                    entry.getValue().prob = (double) counter.get(entry.getKey()) / K;
                }
            }
        }
        //计算不在点覆盖中的点概率，只算直接有边与其相连的点覆盖中的点
        for (Map.Entry<String, Map<String, IndexEntry>> entry : index.entrySet()) {
            String node = entry.getKey();
            if (!coverNodes.contains(node)) {
                for (Edge out : mp.get(node)) {
                    if (coverNodes.contains(out.v)) {
                        entry.getValue().put(out.v,new IndexEntry(out.v, 1, out.prob));
                    }
                }
            }
        }
    }

    int QueryDis(String u, String v) { //询问最短路
        if (coverNodes.contains(u) && coverNodes.contains(v)) { //都在点覆盖中
            if (indexes.get(u).containsKey(v)) return indexes.get(u).get(v).dis;
            return -1;
        } else if (!coverNodes.contains(u) && coverNodes.contains(v)) { //u不在v在
            int minDis = -1;
            for (IndexEntry cv : indexes.get(u).values()) { //枚举u相邻的点覆盖
                    if (indexes.get(cv.v).containsKey(v)) {
                        if (minDis == -1 || cv.dis + indexes.get(cv.v).get(v).dis < minDis) minDis = cv.dis + indexes.get(cv.v).get(v).dis; //取最短路
                    }
            }
            return minDis;
        } else if (coverNodes.contains(u) && !coverNodes.contains(v)) { //v不在u在
            int minDis = -1;
            for (IndexEntry cv : revIndexes.get(v).values()) { //查反向索引
                    if (revIndexes.get(cv.v).containsKey(u)) {
                        if (minDis == -1 || cv.dis + revIndexes.get(cv.v).get(u).dis < minDis) minDis = cv.dis + revIndexes.get(cv.v).get(u).dis;
                    }
            }
            return minDis;
        } else if (!coverNodes.contains(u) && !coverNodes.contains(v)) { //都不在
            int minDis = -1;
            for (IndexEntry cvu : indexes.get(u).values()) { //枚举u相邻的点覆盖
                for (IndexEntry cvv : revIndexes.get(v).values()) { //枚举v相邻的点覆盖，反向索引
                        if (indexes.get(cvu.v).containsKey(cvv.v)) {
                            if (minDis == -1 || cvu.dis + cvv.dis + indexes.get(cvu.v).get(cvv.v).dis < minDis)
                                minDis = cvu.dis + cvv.dis + indexes.get(cvu.v).get(cvv.v).dis;
                        }
                }
            }
            return minDis;
        }
        return -1;
    }

    double QueryProb(String u, String v) { //查询最大可达概率
        if (coverNodes.contains(u) && coverNodes.contains(v)) { //都在点覆盖中
            if (indexes.get(u).containsKey(v)) return indexes.get(u).get(v).prob;
            return -1;
        } else if (!coverNodes.contains(u) && coverNodes.contains(v)) { //u不在v在
            double maxProb = 0;
            for (IndexEntry cv : indexes.get(u).values()) {
                    if (indexes.get(cv.v).containsKey(v)) {
                        if (cv.prob * indexes.get(u).get(cv.v).prob > maxProb) maxProb = cv.prob * indexes.get(u).get(cv.v).prob; //取最大概率
                    }
            }
            return maxProb;
        } else if (coverNodes.contains(u) && !coverNodes.contains(v)) { //v不在u在
            double maxProb = 0;
            for (IndexEntry cv : revIndexes.get(v).values()) {
                    if (revIndexes.get(cv.v).containsKey(u)) {
                        if (cv.prob * revIndexes.get(cv.v).get(u).prob > maxProb) maxProb = cv.prob * revIndexes.get(cv.v).get(u).prob;
                    }
            }
            return maxProb;
        } else if (!coverNodes.contains(u) && !coverNodes.contains(v)) { //都不在
            double maxProb = 0;
            for (IndexEntry cvu : indexes.get(u).values()) {
                for (IndexEntry cvv : revIndexes.get(v).values()) {
                        if (indexes.get(cvu.v).containsKey(cvv.v)) {
                            if (cvu.prob * cvv.prob * indexes.get(cvu.v).get(cvv.v).prob > maxProb) maxProb = cvu.prob * cvv.prob * indexes.get(cvu.v).get(cvv.v).prob;
                        }
                }
            }
            return maxProb;
        }
        return 0;
    }

    void Update(Edge e){
        updateIndex(indexes,revIndexes,e);
        updateIndex(revIndexes,indexes, new Edge(e.v,e.u,e.prob));
    }
    void updateIndex(Map<String, Map<String, IndexEntry>> index,Map<String, Map<String, IndexEntry>> rev, Edge e){
        if (index.get(e.u).containsKey(e.v)){
            index.get(e.u).get(e.v).dis=1;
            index.get(e.u).get(e.v).prob=1-(1-e.prob)*(1-index.get(e.u).get(e.v).prob);
        }
        else {
            index.get(e.u).put(e.v,new IndexEntry(e.v,1,e.prob));
        }
        for (IndexEntry out: index.get(e.v).values()){
            if (index.get(e.u).containsKey(out.v)){
                index.get(e.u).get(out.v).dis=Math.min(index.get(e.u).get(out.v).dis,out.dis+1);
                index.get(e.u).get(out.v).prob=Math.max(index.get(e.u).get(out.v).prob,out.prob*index.get(e.u).get(e.v).prob);
            }
            if (rev.get(out.v).containsKey(e.u)){
                rev.get(out.v).get(e.u).dis=Math.min(rev.get(out.v).get(e.u).dis,out.dis+1);
                rev.get(out.v).get(e.u).prob=Math.max(rev.get(out.v).get(e.u).prob,out.prob*index.get(e.u).get(e.v).prob);
            }
        }
    }
    public static void main(String[] args) throws Exception {
//        File f = new File(".\\dataset\\hep\\graph.txt");
        File f = new File(".\\in.txt");
        Scanner s = new Scanner(f);
        List<Edge> l = new LinkedList<Edge>();
        int maxLine = 100;
        while (true) {
            try {
                int u, v;
                double p;
                u = s.nextInt();
                v = s.nextInt();
                p = s.nextDouble();
                Edge e = new Edge(Integer.toString(u), Integer.toString(v), p);
                l.add(e);
                --maxLine;
                if (maxLine == 0) break;
            } catch (NoSuchElementException e) {
                break;
            }
        }
        Index x = new Index();
        x.Init(l);
        Scanner std = new Scanner(System.in);
        while (true) {
            int op = std.nextInt();
            int a = std.nextInt();
            int b = std.nextInt();
            if (op==0){
                double p=std.nextDouble();
                x.Update(new Edge(Integer.toString(a),Integer.toString(b),p));
            }
            else {
                System.out.println(x.QueryDis(Integer.toString(a), Integer.toString(b)));
                System.out.println(x.QueryProb(Integer.toString(a), Integer.toString(b)));
            }
        }
    }
}
