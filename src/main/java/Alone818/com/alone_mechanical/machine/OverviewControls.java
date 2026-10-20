package Alone818.com.alone_mechanical.machine;
/** Tap Left Shift toggles free view; a Shift+movement chord instead selects player follow. */
public final class OverviewControls {
    private boolean active,free,rightWas,leftWas,chord;
    public boolean active(){return active;}
    public boolean free(){return active&&free;}
    public void reset(){active=free=rightWas=leftWas=chord=false;}
    public void releaseKeys(){rightWas=leftWas=chord=false;}
    public void update(boolean right,boolean left,boolean movement){
        if(right&&!rightWas){active=!active;free=false;chord=left;rightWas=right;leftWas=left;return;}
        if(active){
            if(left&&!leftWas)chord=movement;
            if(left&&movement){chord=true;free=false;}
            if(!left&&leftWas&&!chord)free=!free;
        }
        rightWas=right;leftWas=left;
    }
}
