package cz.cvut.uhlirad1.homemyo.service.tree;

import com.thalmic.myo.Pose;
import cz.cvut.uhlirad1.homemyo.knx.Command;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Adam Uhlíř <uhlir.a@gmail.com>
 * Date: 19.12.14
 */
public class Node {

    private Pose pose;

    private Command command;

    private Map<Pose, Node> childrens;

    public Node() {
        childrens = new HashMap<Pose, Node>();
    }

    public Node(Pose pose) {
        this.pose = pose;
        childrens = new HashMap<Pose, Node>();
    }

    public Node(Pose pose, Command command) {
        this.pose = pose;
        this.command = command;
        childrens = new HashMap<Pose, Node>();
    }

    public void addChild(Pose pose, Node node){
        childrens.put(pose, node);
    }

    public Node getChild(Pose pose){
        return childrens.get(pose);
    }

    public boolean hasCommand(){
        return command != null;
    }

    public Command getCommand(){
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }
}
