public class IndexEntry { //索引项
    String v; //到哪个点
    int dis; //距离
    double prob; //概率
    IndexEntry(String V,int D,double P){
        v=V; dis=D; prob=P;
    }
}
