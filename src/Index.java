import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class Index {
    private static final int K = 100;
    Map<String, List<IndexEntry>> indexes, revIndexes; //每个点的正反索引
    Set<String> coverNodes; //点覆盖集合

    void Init(List<Edge> l) { //初始化
        coverNodes = new HashSet<String>();

        GenerateCover(l); //生成点覆盖
        indexes = new HashMap<String, List<IndexEntry>>();
        GenerateIndexes(l, indexes); //生成索引
        revIndexes = new HashMap<String, List<IndexEntry>>();
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

    void GenerateIndexes(List<Edge> l, Map<String, List<IndexEntry>> index) {
        for (Edge e : l) {
            index.computeIfAbsent(e.u, k -> new LinkedList<IndexEntry>()); //初始化
            index.computeIfAbsent(e.v, k -> new LinkedList<IndexEntry>());
        }
        Map<String, Set<Edge>> mp = new HashMap<String, Set<Edge>>(); //每个点连出哪些边
        for (Edge e : l) {
            Set<Edge> st = mp.computeIfAbsent(e.u, k -> new HashSet<Edge>());
            st.add(e);
            mp.computeIfAbsent(e.v, k -> new HashSet<Edge>());
        }

        for (Map.Entry<String, List<IndexEntry>> entry : index.entrySet()) {
            entry.getValue().add(new IndexEntry(entry.getKey(), 0, 1)); //自己到自己
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
                            index.get(node).add(new IndexEntry(out.v, dis.get(out.v), 0));
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
            for (IndexEntry entry : index.get(node)) {
                if (!entry.v.equals(node) && counter.containsKey(entry.v)) {
                    entry.prob = (double) counter.get(entry.v) / K;
                }
            }
        }
        //计算不在点覆盖中的点概率，只算直接有边与其相连的点覆盖中的点
        for (Map.Entry<String, List<IndexEntry>> entry : index.entrySet()) {
            String node = entry.getKey();
            if (!coverNodes.contains(node)) {
                for (Edge out : mp.get(node)) {
                    if (coverNodes.contains(out.v)) {
                        entry.getValue().add(new IndexEntry(out.v, 1, out.prob));
                    }
                }
            }
        }
    }

    int QueryDis(String u, String v) { //询问最短路
        if (coverNodes.contains(u) && coverNodes.contains(v)) { //都在点覆盖中
            for (IndexEntry out : indexes.get(u)) {
                if (out.v.equals(v)) {
                    return out.dis;
                }
            }
        } else if (!coverNodes.contains(u) && coverNodes.contains(v)) { //u不在v在
            int minDis = -1;
            for (IndexEntry cv : indexes.get(u)) { //枚举u相邻的点覆盖
                for (IndexEntry out : indexes.get(cv.v)) {
                    if (out.v.equals(v)) {
                        if (minDis == -1 || cv.dis + out.dis < minDis) minDis = cv.dis + out.dis; //取最短路
                    }
                }
            }
            return minDis;
        } else if (coverNodes.contains(u) && !coverNodes.contains(v)) { //v不在u在
            int minDis = -1;
            for (IndexEntry cv : revIndexes.get(v)) { //查反向索引
                for (IndexEntry out : revIndexes.get(cv.v)) {
                    if (out.v.equals(u)) {
                        if (minDis == -1 || cv.dis + out.dis < minDis) minDis = cv.dis + out.dis;
                    }
                }
            }
            return minDis;
        } else if (!coverNodes.contains(u) && !coverNodes.contains(v)) { //都不在
            int minDis = -1;
            for (IndexEntry cvu : indexes.get(u)) { //枚举u相邻的点覆盖
                for (IndexEntry cvv : revIndexes.get(v)) { //枚举v相邻的点覆盖，反向索引
                    for (IndexEntry out : indexes.get(cvu.v)) {
                        if (out.v.equals(cvv.v)) {
                            if (minDis == -1 || cvu.dis + cvv.dis + out.dis < minDis)
                                minDis = cvu.dis + cvv.dis + out.dis;
                            ;
                        }
                    }
                }
            }
            return minDis;
        }
        return -1;
    }

    double QueryProb(String u, String v) { //查询最大可达概率
        if (coverNodes.contains(u) && coverNodes.contains(v)) { //都在点覆盖中
            for (IndexEntry out : indexes.get(u)) {
                if (out.v.equals(v)) {
                    return out.prob;
                }
            }
        } else if (!coverNodes.contains(u) && coverNodes.contains(v)) { //u不在v在
            double maxProb = 0;
            for (IndexEntry cv : indexes.get(u)) {
                for (IndexEntry out : indexes.get(cv.v)) {
                    if (out.v.equals(v)) {
                        if (cv.prob * out.prob > maxProb) maxProb = cv.prob * out.prob; //取最大概率
                    }
                }
            }
            return maxProb;
        } else if (coverNodes.contains(u) && !coverNodes.contains(v)) { //v不在u在
            double maxProb = 0;
            for (IndexEntry cv : revIndexes.get(v)) {
                for (IndexEntry out : revIndexes.get(cv.v)) {
                    if (out.v.equals(u)) {
                        if (cv.prob * out.prob > maxProb) maxProb = cv.prob * out.prob;
                    }
                }
            }
            return maxProb;
        } else if (!coverNodes.contains(u) && !coverNodes.contains(v)) { //都不在
            double maxProb = 0;
            for (IndexEntry cvu : indexes.get(u)) {
                for (IndexEntry cvv : revIndexes.get(v)) {
                    for (IndexEntry out : indexes.get(cvu.v)) {
                        if (out.v.equals(cvv.v)) {
                            if (cvu.prob * cvv.prob * out.prob > maxProb) maxProb = cvu.prob * cvv.prob * out.prob;
                            ;
                        }
                    }
                }
            }
            return maxProb;
        }
        return 0;
    }

    public static void main(String[] args) throws Exception {
        File f = new File(".\\dataset\\hep\\graph.txt");
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
            int a = std.nextInt();
            int b = std.nextInt();
            System.out.println(x.QueryDis(Integer.toString(a), Integer.toString(b)));
            System.out.println(x.QueryProb(Integer.toString(a), Integer.toString(b)));
        }
    }
}
