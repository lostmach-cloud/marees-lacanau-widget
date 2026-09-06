package fr.manu.mareeslacanau;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShomClient {

    private static final String SOURCE =
        "https://services.data.shom.fr/hdm/vignette/petite/LACANAU?locale=fr";

    public static List<Tide> fetch() throws Exception {

        HttpURLConnection connection =
            (HttpURLConnection) new URL(SOURCE).openConnection();

        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty(
            "User-Agent",
            "MareesLacanauWidget/1.0 Android"
        );

        StringBuilder raw = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    connection.getInputStream(),
                    StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                raw.append(line).append('\n');
            }
        }

        return parse(raw.toString());
    }

    static List<Tide> parse(String input) {

        String s = input
            .replace("\\\"", "\"")
            .replace("\\n", " ")
            .replace("&nbsp;", " ")
            .replace(",", ".");

        s = s
            .replaceAll("(?is)<br\\s*/?>", " | ")
            .replaceAll("(?is)</tr>", " || ")
            .replaceAll("(?is)</td>", " | ")
            .replaceAll("(?is)<[^>]+>", " ")
            .replaceAll("\\s+", " ");

        List<Tide> result = new ArrayList<>();

        Pattern p = Pattern.compile(
            "(PM|BM).*?([0-2]?\\d[:h][0-5]\\d).*?" +
            "([0-9]+(?:\\.[0-9]+)?)\\s*m?" +
            "(?:.*?\\b([2-9]\\d|1[01]\\d|120)\\b)?",
            Pattern.CASE_INSENSITIVE
        );

        Matcher m = p.matcher(s);

        while (m.find() && result.size() < 4) {

            String type = m.group(1).toUpperCase();
            String time = m.group(2).replace('h', ':');

            if (time.length() == 4) {
                time = "0" + time;
            }

            String height = m.group(3) + " m";

            String coef =
                m.group(4) == null ? "" : m.group(4);

            if ("BM".equals(type)) {
                coef = "";
            }

            result.add(
                new Tide(type, time, height, coef)
            );
        }

        if (result.isEmpty()) {
            throw new IllegalStateException(
                "Format SHOM non reconnu"
            );
        }

        return result;
    }
}
