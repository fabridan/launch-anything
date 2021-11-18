package bar.tile;

import bar.logic.Settings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class TileManager {

    private final Settings settings;
    private final List<Tile> tiles = new ArrayList<>();
    private final List<Tile> generatedTiles = new ArrayList<>();
    private final List<TileGenerator> tileGenerators = new ArrayList<>();
    private final List<InputEvaluatedListener> onInputEvaluatedListeners = new ArrayList<>();
    private final List<TileCategory> categories = new ArrayList<>();
    private File tileFile;

    public TileManager(Settings settings) {
        this.settings = settings;
        findSettingsFile();
        if (tileFile == null) tileFile = new File("res/tiles.json");
        else readTilesFromFile();
        tiles.add(new Tile(new JSONObject("{\"exportable\":false,\"keywords\":\"edit\",\"id\":\"settingsWeb\",\"label\":\"LaunchAnything Settings\",\"category\":\"settings\",\"isActive\":true,\"actions\":[{\"type\":\"settingsWeb\"}]}")));
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture = null;

    public void evaluateUserInput(String input) {
        if (currentFuture != null) currentFuture.cancel(true);
        if (input.length() <= 1) {
            setEvaluationResults(new ArrayList<>());
        } else {
            currentFuture = evaluate(input);
        }
    }

    private void setEvaluationResults(List<Tile> tiles) {
        onInputEvaluatedListeners.forEach(listener -> listener.onInputEvaluated(tiles));
    }

    private Future<?> evaluate(String input) {
        return executor.submit(() -> {
            setEvaluationResults(
                    tiles.stream()
                            .filter(tile -> tile.matchesSearch(input))
                            .sorted(Comparator.comparing(Tile::getLastActivated).reversed())
                            .collect(Collectors.toList())
            );
            return null;
        });
    }

    private final static String[] possibleTilesFiles = {
            "tiles.json",
            "res/tiles.json",
            "../tiles.json",
            "../res/tiles.json"
    };

    private void findSettingsFile() {
        for (String possibleSettingsFile : possibleTilesFiles) {
            File candidate = new File(possibleSettingsFile).getAbsoluteFile();
            if (candidate.exists()) {
                tileFile = candidate;
                return;
            }
        }
        tileFile = null;
    }

    private void readTilesFromFile() {
        try {
            StringBuilder fileContent = new StringBuilder();
            Scanner reader = new Scanner(tileFile);
            while (reader.hasNextLine()) {
                fileContent.append(reader.nextLine().trim());
            }
            reader.close();

            JSONObject tilesRoot = new JSONObject(fileContent.toString());
            JSONArray tilesArray = tilesRoot.optJSONArray("tiles");
            if (tilesArray != null) {
                for (int i = 0; i < tilesArray.length(); i++) {
                    JSONObject tileJson = tilesArray.optJSONObject(i);
                    if (tileJson == null) continue;
                    Tile tile = new Tile(tileJson);
                    if (tile.isValid()) tiles.add(tile);
                }
            }

            JSONArray tileGeneratorsArray = tilesRoot.optJSONArray("tile-generators");
            if (tileGeneratorsArray != null) {
                for (int i = 0; i < tileGeneratorsArray.length(); i++) {
                    JSONObject tileGeneratorJson = tileGeneratorsArray.optJSONObject(i);
                    if (tileGeneratorJson == null) continue;
                    TileGenerator tileGenerator = new TileGenerator(tileGeneratorJson, true);
                    if (tileGenerator.isValid()) tileGenerators.add(tileGenerator);
                }
            }

            JSONArray categoriesArray = tilesRoot.optJSONArray("categories");
            if (categoriesArray != null) {
                for (int i = 0; i < categoriesArray.length(); i++) {
                    JSONObject categoryJson = categoriesArray.optJSONObject(i);
                    if (categoryJson == null) continue;
                    TileCategory category = new TileCategory(categoryJson);
                    if (category.isValid()) {
                        categories.add(category);
                    }
                }
            }

            System.out.println("Loaded " + tiles.size() + " tile(s), " + tileGenerators.size() + " tile generator(s) and " + categories.size() + " category/ies.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void regenerateGeneratedTiles() {
        generatedTiles.clear();
        for (TileGenerator tileGenerator : tileGenerators) {
            generatedTiles.addAll(tileGenerator.generateTiles());
        }
    }

    public void addCategory(TileCategory category) {
        categories.add(category);
    }

    public void removeCategory(TileCategory category) {
        categories.remove(category);
    }

    public List<TileCategory> getCategories() {
        return categories;
    }

    public TileCategory findCategory(String label) {
        for (TileCategory category : categories) {
            if (category.getLabel().equals(label)) return category;
        }
        return null;
    }

    public void addTile(Tile tile) {
        tiles.add(tile);
    }

    public Tile findTile(String tileId) {
        return tiles.stream().filter(tile -> tile.getId().equals(tileId)).findFirst().orElse(null);
    }

    public void removeTile(Tile tile) {
        tiles.remove(tile);
    }

    public void save() {
        try {
            tileFile.getParentFile().mkdirs();
            FileWriter myWriter = new FileWriter(tileFile);

            myWriter.write(toJSON().toString());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject toJSON() {
        JSONArray tilesArray = new JSONArray();
        for (Tile tile : tiles) {
            if (tile.isExportable())
                tilesArray.put(tile.toJSON());
        }

        JSONArray tileGeneratorsArray = new JSONArray();
        for (TileGenerator tileGenerator : tileGenerators) {
            tileGeneratorsArray.put(tileGenerator.toJSON());
        }

        JSONArray categoriesArray = new JSONArray();
        for (TileCategory category : categories) {
            categoriesArray.put(category.toJSON());
        }

        JSONObject tilesRoot = new JSONObject();
        tilesRoot.put("tiles", tilesArray);
        tilesRoot.put("tile-generators", tileGeneratorsArray);
        tilesRoot.put("categories", categoriesArray);

        return tilesRoot;
    }

    public void addOnInputEvaluatedListener(InputEvaluatedListener listener) {
        onInputEvaluatedListeners.add(listener);
    }

    public interface InputEvaluatedListener {
        void onInputEvaluated(List<Tile> tiles);
    }
}
