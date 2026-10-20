package Alone818.com.alone_mechanical.machine;
/** Cardinal camera headings and player-relative movement without changing the player's look. */
public final class OverviewOrientation {
    private OverviewOrientation(){}
    private static float wrap(float yaw){float value=yaw%360;return value>=180?value-360:value< -180?value+360:value;}
    public static float snap(float yaw){return Float.isFinite(yaw)?wrap(Math.round(yaw/90f)*90f):0;}
    public static float turn(float yaw,int quarters){return snap(yaw+Math.floorMod(quarters,4)*90f);}
    public record Movement(float forward,float left){}
    public static Movement movement(float forward,float left,float cameraYaw,float playerYaw){
        double delta=Math.toRadians(cameraYaw-playerYaw),cos=Math.cos(delta),sin=Math.sin(delta);
        return new Movement((float)(forward*cos+left*sin),(float)(left*cos-forward*sin));
    }
}
