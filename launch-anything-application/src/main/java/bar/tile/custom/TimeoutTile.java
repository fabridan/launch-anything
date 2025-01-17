package bar.tile.custom;

import bar.tile.Tile;
import bar.tile.TileAction;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class TimeoutTile implements RuntimeTile {

    @Override
    public List<Tile> generateTiles(String search, AtomicReference<Long> lastInputEvaluated) {
        if (search.matches("(?:timeout|to) ?\\d+")) {
            int minutes = Integer.parseInt(search.replaceAll("(?:timeout|to) ?(\\d+)", "$1").trim());
            Tile tile = new Tile("Timeout for " + minutes + " minute" + (minutes == 1 ? "" : "s"));
            tile.setCategory("runtime");
            TileAction action = new TileAction("settings", "timeout", minutes + "");
            tile.addAction(action);
            return Collections.singletonList(tile);
        }
        return Collections.emptyList();
    }

    public static String getTitle() {
        return "Timeout";
    }

    public static String getDescription() {
        return "'timeout' or 'to' and a duration in minutes the bar should remain inactive.";
    }
}
