package com.netflix.kayenta.mannwhitney;

public class CWilcoxJava {

    final private int WILCOX_MAX = 50;
    private double[][][] w;

    public CWilcoxJava(int m, int n) {
        m = Math.max(m, WILCOX_MAX);
        n = Math.max(n, WILCOX_MAX);
        w = new double[m + 1][n + 1][((m * n)/2) + 1];
        this.init();
    }

    public void init() {
        for (int i = 0; i < w.length; i++) {
            for (int j = 0; j < w[i].length; j++) {
                for (int k = 0; k < w[i][j].length; k++) {
                    w[i][j][k] = -1.0;
                }
            }
        }
    }

    public double cwilcox(int k, int m, int n) {
        int u = m * n;
        if (k < 0 || k > u) return 0.0;

        double c = (u / 2);
        if (k > c) k = u - k;
        int i = n;
        int j = m;
        if (m < n) {
            i = m;
            j = n;
        }

        if (j == 0){
            if (k == 0) return 1.0;
            else return 0.0;
        }

        if (j > 0 && k < j) return cwilcox(k, i, k);

        this.updateCube(i, j, k);

        return w[i][j][k];
    }

    public void updateCube(int i, int j, int k) {
        if (w[i][j][k] < 0) {
            if (j == 0) {
                if (k == 0) {
                    w[i][j][k] = 1.0;
                }
                else {
                    w[i][j][k] = 2.0;
                }
            }
            else {
                w[i][j][k] = this.cwilcox(k - j, i - 1, j) + this.cwilcox(k, i, j - 1);
            }
        }
    }
}
