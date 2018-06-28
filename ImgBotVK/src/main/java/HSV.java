import java.awt.*;

public class HSV {
    float h;
    float s;
    float v;
    int r;
    int g;
    int b;
    float[][] hsvArray;
    String URL;
    Histo histo;



    public static HSV getHSV(int red,int green,int blue){
        return new HSV(red,green,blue);
    }
    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public HSV(float h, float s, float v) {
        this.h = h;
        this.s = s;
        this.v = v;
    }

    public HSV(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
        float[] hsv = new float[3];
        Color.RGBtoHSB(r,g,b,hsv);
        this.h=hsv[0];
        this.s=hsv[1];
        this.v=hsv[2];
    }
    public HSV(int[][] pallete,String URL){
        hsvArray=new float[pallete.length][3];
        this.URL=URL;

        for (int i = 0; i < pallete.length; i++) {
            float[] hsv1 = new float[3];
            Color.RGBtoHSB(pallete[i][0],pallete[i][1],pallete[i][2],hsv1);
            hsvArray[i][0] = hsv1[0];
            hsvArray[i][1] = hsv1[1];
            hsvArray[i][2] = hsv1[2];
        }

    }
    public HSV(Histo histo,String URL){
        this.URL=URL;
        this.histo=histo;
    }

    public Histo getHisto() {
        return histo;
    }

    public float getH() {
        return h;
    }

    public void setH(float h) {
        this.h = h;
    }

    public float getS() {
        return s;
    }

    public void setS(float s) {
        this.s = s;
    }

    public float getV() {
        return v;
    }

    public void setV(float v) {
        this.v = v;
    }

    public float[][] getHsvArray() {
        return hsvArray;
    }
}
