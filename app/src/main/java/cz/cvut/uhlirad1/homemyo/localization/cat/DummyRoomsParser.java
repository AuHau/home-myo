package cz.cvut.uhlirad1.homemyo.localization.cat;

import cz.cvut.uhlirad1.homemyo.localization.IRoomsParser;
import cz.cvut.uhlirad1.homemyo.localization.Room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by adam on 1.12.14.
 */
// TODO: Implementovat XML parser
public class DummyRoomsParser implements IRoomsParser {

    private HashMap<String, Room> mapping;

    @Override
    public List<Room> parse(){
        ArrayList list = new ArrayList<Room>();
        Room r0 = new Room(0, "Celý byt");
        Room r1 = new Room(1, "Zasedačka (304a)");
        Room r2 = new Room(2, "Kuchyně (304b)");
        Room r3 = new Room(3, "Pokoj (304c)");
        Room r4 = new Room(4, "Koupelna (304d)");


        list.add(r1.getId(), r0);
        list.add(r1.getId(), r1);
        list.add(r2.getId(), r2);
        list.add(r3.getId(), r3);
        list.add(r4.getId(), r4);


        mapping = new HashMap<String, Room>();
        mapping.put("304a", r1);
        mapping.put("304b", r2);
        mapping.put("304c", r3);
        mapping.put("304d", r4);


        return list;
    }

    @Override
    public HashMap parseMapping() {
        return mapping;
    }
}