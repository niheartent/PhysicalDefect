package PhysicalDefect.patches;

public class AlmightyPatch {

    public static final java.util.HashSet<String> ALMIGHTY_CARDLIST = new java.util.HashSet<>(java.util.Arrays.asList(
            "Gash", // 爪击
            "Rampage" // 暴走
    ));

    public static boolean isAlmightyBlacklisted(String cardID) {
        return ALMIGHTY_CARDLIST.contains(cardID);
    }

}