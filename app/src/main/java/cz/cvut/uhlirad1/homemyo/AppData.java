package cz.cvut.uhlirad1.homemyo;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import com.thalmic.myo.Pose;
import cz.cvut.uhlirad1.homemyo.knx.Command;
import cz.cvut.uhlirad1.homemyo.knx.CommandParserFactory;
import cz.cvut.uhlirad1.homemyo.knx.ICommandParser;
import cz.cvut.uhlirad1.homemyo.localization.IRoomParser;
import cz.cvut.uhlirad1.homemyo.localization.Room;
import cz.cvut.uhlirad1.homemyo.localization.RoomParserFactory;
import cz.cvut.uhlirad1.homemyo.service.tree.Combo;
import cz.cvut.uhlirad1.homemyo.service.tree.MyoPose;
import cz.cvut.uhlirad1.homemyo.service.tree.Node;
import cz.cvut.uhlirad1.homemyo.service.tree.TreeParser;
import cz.cvut.uhlirad1.homemyo.settings.AppPreferences_;
import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class that manage all data in application.
 *
 * @author: Adam Uhlíř <uhlir.a@gmail.com>
 */
@EBean(scope = EBean.Scope.Singleton)
public class AppData {

    /**
     * Tree root of tree format.
     */
    private Map<Integer, Node> rootTree;

    /**
     * Tree root of list of rooms format
     */
    private List<Room> rootRooms;


    private Map<Integer, Command> commands;
    private Map<Integer, Room> rooms;
    private Map<String, Room> roomMapping;

    private IRoomParser roomParser;
    private ICommandParser commandParser;
    private TreeParser treeParser;

    /**
     * Variable for storing highest Combo Id in Tree config.
     * It is used for creating new combos.
     */
    private int highestComboId = 0;
    private String errorMsg;

    @Pref
    protected AppPreferences_ preferences;

    @RootContext
    protected Context context;

    /**
     * Method will load and parse all necessary configuration files
     * for application run.
     * Also it is method which is caching all Exceptions from parsers and so on.
     */
    @AfterInject
    void init(){
        errorMsg = "";
        roomParser = RoomParserFactory.createParser();
        commandParser = CommandParserFactory.createCommandParser();

        try {
            parseRooms();
            parseCommands();

            File treeConfig = getTreeConfig();
            if (treeConfig != null) {
                treeParser = new TreeParser();
                rootRooms = treeParser.parse(treeConfig);
                rootTree = new HashMap<Integer, Node>();
                transferListToTree();
            }
        } catch (IllegalStateException e) {
            Log.e("AppData", "Error! " + e.getMessage());
            setErrorMsg(e.getMessage());
        } catch (Exception e) {
            Log.e("AppData", "Error! " + e.getMessage());
            setErrorMsg(e.getMessage());
        }
    }

    /**
     * Method which defines if during loading and parsing
     * was any error and therefore if data are valid.
     * @return true if data are valid, otherwise false.
     */
    public boolean areDataValid() {
        return rootRooms != null && commands != null && rooms != null;
    }

    /**
     * Method will return File instance of Rooms config
     * @return Rooms config
     */
    private File getRoomsConfig() {
        File configDir = new File(Environment.getExternalStorageDirectory() + File.separator + preferences.applicationFolder().get());

        if (!configDir.exists() && !configDir.mkdirs()) {
            Log.e("AppData", "Config directory does not exist and can not be created!");
            throw new IllegalStateException("Config directory does not exist.");
        }

        File config = new File(configDir, preferences.roomConfig().get());
        if (!config.exists()) {
            Log.e("AppData", "Rooms config can not be found!");
            throw new IllegalStateException("Rooms configuration file can not be found.");
        }

        return config;
    }

    /**
     * Method will parse Rooms config.
     * If there is some error during parsing, Exception is thrown.
     *
     * @throws Exception
     */
    private void parseRooms() throws Exception {
        File config = getRoomsConfig();
        if (config != null) {
            rooms = roomParser.parse(config);
            roomMapping = roomParser.parseMapping();
        }
    }

    /**
     * Method will save actual rooms variable into Rooms config.
     */
    public void commitRooms() {
        try {
            File config = getRoomsConfig();
            roomParser.save(config, rooms);
        } catch (Exception e) {
            setErrorMsg("Error! Rooms were not saved!");
        }
    }

    /**
     * Method will return File instance of Commands config
     * @return Commands config
     */
    private File getCommandsConfig() {
        File configDir = new File(Environment.getExternalStorageDirectory() + File.separator + preferences.applicationFolder().get());

        if (!configDir.exists() && !configDir.mkdirs()) {
            Log.e("AppData", "Config directory does not exist and can not be created!");
            throw new IllegalStateException("Config directory does not exist and can not be created.");
        }

        File config = new File(configDir, preferences.commandConfig().get());
        if (!config.exists()) {
            Log.e("AppData", "Commands config can not be found!");
            throw new IllegalStateException("Commands configuration file can not be found.");
        }

        return config;
    }

    /**
     * Method will parse Commands config.
     * If there is some error during parsing, Exception is thrown.
     *
     * @throws Exception
     */
    private void parseCommands() throws Exception {
        File config = getCommandsConfig();
        if (config != null) commands = commandParser.parse(config);
    }

    /**
     * Method will save actual commands variable into Commands config.
     */
    public void commitCommands() {
        try {
            File config = getCommandsConfig();
            commandParser.save(config, commands);
        } catch (Exception e) {
            setErrorMsg("Error! Commands were not saved!");
        }
    }

    /**
     * Method will return File instance of Tree config
     * @return Tree config
     */
    private File getTreeConfig() {
        File config = new File(context.getExternalFilesDir(null), preferences.treeConfig().get());

        if (!config.exists()) {
            try {
                config.createNewFile();
                Log.i("AppData", "Tree config was created!");
            } catch (IOException e) {
                setErrorMsg("ERROR - " + e.getLocalizedMessage());
                Log.e("AppData", e.getLocalizedMessage());
                return null;
            }
        }

        return config;
    }

    /**
     * Method will save actual rootRooms variable into Commands config.
     */
    public void commitTree() {
        File config = getTreeConfig();
        if (config != null) {
            try {
                treeParser.save(config, rootRooms);
            } catch (Exception e) {
                setErrorMsg("Error! Tree were not saved!");
            }
            transferListToTree();
        }
    }

    /**
     * Method will return Combo specified by id;
     * @param id
     * @return Combo
     */
    public Combo getCombo(int id) {
        return getCombo(id, -1);
    }

    /**
     * Method will return Combo specified by id and roomId;
     * @param id
     * @param roomId
     * @return
     */
    public Combo getCombo(int id, int roomId) {
        if (roomId >= 0) {
            Room room = findRoom(id, rootRooms);
            for (Combo combo : room.getCombo()) {
                if (combo.getId() == id) return combo;
            }
        } else {
            for (Room room : rootRooms) {
                for (Combo combo : room.getCombo()) {
                    if (combo.getId() == id) return combo;
                }
            }
        }
        return null;
    }

    /**
     * Method will remove Combo specified by id, from rootRooms.
     * Also if the Combo is alone in room tree, the tree will be deleted.
     *
     * @param id
     */
    public void removeCombo(int id) {
        int pos, roomPos = 0, deleteRoom = -1, foundPosition = -1;
        for (Room room : rootRooms) {
            pos = 0;

            for (Combo combo : room.getCombo()) {
                if (combo.getId() == id) {
                    foundPosition = pos;
                    break;
                }
            }

            if (foundPosition >= 0) {
                if (room.getCombo().size() == 1) {
                    deleteRoom = roomPos;
                } else {
                    room.getCombo().remove(foundPosition);
                }
                break;
            }

            roomPos++;
        }

        if (deleteRoom >= 0) {
            rootRooms.remove(deleteRoom);
        }
    }

    /**
     * Method will add Combo specified in combo, to room tree specified by roomId
     *
     * @param combo
     * @param roomId
     */
    public void addCombo(Combo combo, int roomId) {
        boolean added = false;
        for (Room room : rootRooms) {
            if (room.getId() == roomId) {
                room.getCombo().add(combo);
                added = true;
            }
        }

        // Room is not currently in rootRooms
        if (!added) {
            Room room = new Room(roomId);
            ArrayList<Combo> list = new ArrayList<Combo>();
            list.add(combo);
            room.setCombo(list);

            // Smart home room should be first
            if (roomId == 0) {
                rootRooms.add(0, room);
            } else
                rootRooms.add(room);
        }
    }

    /**
     * Method will move Combo from its original room tree to room tree specified by toRoomId
     * @param movedCombo
     * @param toRoomId
     */
    public void moveCombo(Combo movedCombo, int toRoomId) {
        int pos;
        boolean moved = false;
        for (Room room : rootRooms) {
            pos = 0;
            for (Combo combo : room.getCombo()) {
                if (combo.getId() == movedCombo.getId()) {
                    room.getCombo().remove(pos);
                    break;
                }
            }

            if (room.getId() == toRoomId) {
                moved = true;
                room.getCombo().add(movedCombo);
            }
        }

        // Room is not currently in tree
        if (!moved) {
            Room room = new Room(toRoomId);
            ArrayList<Combo> list = new ArrayList<Combo>();
            list.add(movedCombo);
            room.setCombo(list);

            // Whole flat room should be first
            if (toRoomId == 0) {
                rootRooms.add(0, room);
            } else
                rootRooms.add(room);
        }
    }

    /**
     * Method will return highest Combo Id which was found in rootTree and increase its value.
     * @return
     */
    public int getHighestComboIdAndRaise() {
        return ++highestComboId;
    }


    private Room findRoom(int id, Map<Integer, Room> roomMap) {
        Room room;
        for (Map.Entry entryRoom : roomMap.entrySet()) {
            room = (Room) entryRoom.getValue();
            if (id == room.getId()) return room;
        }
        return null;
    }

    private Room findRoom(int id, List<Room> roomList) {
        for (Room room : roomList) {
            if (id == room.getId()) return room;
        }
        return null;
    }


    /**
     * Method will transform rootRooms into rootTree, which has tree format.
     */
    private void transferListToTree() {
        for (Room room : rootRooms) {
            if (room.getCombo() != null)
                processRoom(rootTree, room);
        }
    }

    private void processRoom(Map<Integer, Node> map, Room room) {
        Node root = new Node();
        map.put(room.getId(), root);

        for (Combo combo : room.getCombo()) {
            if (combo.getId() > highestComboId) highestComboId = combo.getId();
            processCombo(root, combo.getCommandId(), combo.getMyoPose(), 0);
        }
    }

    private void processCombo(Node tree, int commandId, List<MyoPose> poses, int actualPose) {

        // All poses have been already applied, save command
        if (actualPose >= poses.size()) {
            tree.setCommand(commands.get(commandId));
            return;
        }

        // If there is already existing Node with pose, follow that path otherwise create new Node
        Pose pose = poses.get(actualPose).getType();
        if (tree.getChild(pose) != null) {
            processCombo(tree.getChild(pose), commandId, poses, ++actualPose);
        } else {
            Node node = new Node(pose);
            tree.addChild(pose, node);
            processCombo(node, commandId, poses, ++actualPose);
        }
    }

    private void setErrorMsg(String text) {
        // Show toast for errors which does not cause error crash.
        if (commands == null || rooms == null || rootRooms == null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }

        errorMsg = text;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public Map<Integer, Node> getRootTree() {
        return rootTree;
    }

    public void setRootTree(Map<Integer, Node> rootTree) {
        this.rootTree = rootTree;
    }

    public List<Room> getRootRooms() {
        return rootRooms;
    }

    public void setRootRooms(List<Room> rootRooms) {
        this.rootRooms = rootRooms;
    }

    public Map<Integer, Command> getCommands() {
        return commands;
    }

    public void setCommands(Map<Integer, Command> commands) {
        this.commands = commands;
    }

    public Map<Integer, Room> getRooms() {
        return rooms;
    }

    public void setRooms(Map<Integer, Room> rooms) {
        this.rooms = rooms;
    }

    public Map<String, Room> getRoomMapping() {
        return roomMapping;
    }
}
