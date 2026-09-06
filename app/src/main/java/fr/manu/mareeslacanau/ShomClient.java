package fr.manu.mareeslacanau;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class ShomClient {

    private static final String SOURCE =
        "https://www.lacanausurfinfo.com/";

    public static List<Tide> fetch() throws Exception {

        OkHttpClient bootstrapClient =
            new OkHttpClient.Builder().build();

        DnsOverHttps dns =
            new DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(
                    HttpUrl.get(
                        "https://cloudflare-dns.com/dns-query"
                    )
                )
                .post(true)
                .build();

        OkHttpClient client =
            new OkHttpClient.Builder()
                .dns(dns)
                .build();

        Request request =
            new Request.Builder()
                .url(SOURCE)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Android) MareesLacanauWidget/1.0"
                )
                .header(
                    "Accept-Language",
                    "fr-FR,fr;q=0.9"
                )
                .build();

        try (Response response =
                 client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new IllegalStateException(
                    "HTTP " + response.code()
                );
            }

            ResponseBody body = response.body();

            if (body == null) {
                throw new IllegalStateException(
                    "Réponse vide"
                );
            }

            return parse(body.string());
        }
    }

    static List<Tide> parse(String html) {

        Pattern tablePattern =
            Pattern.compile(
                "(?is)<div[^>]*bouees_row__mareesbox[^>]*>(.*?)</table>"
            );

        Matcher tableMatcher =
            tablePattern.matcher(html);

        if (!tableMatcher.find()) {
            throw new IllegalStateException(
                "Tableau des marées introuvable"
            );
        }

        String table =
            tableMatcher.group(1);

        String[] coeffs =
            extractRow(table, "COEFF");

        String[] pleineMer =
            extractRow(table, "PLEINE MER");

        String[] basseMer =
            extractRow(table, "BASSE MER");

        List<Tide> result =
            new ArrayList<>();

        result.add(
            new Tide(
                "PM",
                normalizeTime(pleineMer[0]),
                "",
                coeffs[0]
            )
        );

        result.add(
            new Tide(
                "BM",
                normalizeTime(basseMer[0]),
                "",
                ""
            )
        );

        result.add(
            new Tide(
                "PM",
                normalizeTime(pleineMer[1]),
                "",
                coeffs[1]
            )
        );

        result.add(
            new Tide(
                "BM",
                normalizeTime(basseMer[1]),
                "",
                ""
            )
        );

        return result;
    }

    private static String[] extractRow(
        String table,
        String label
    ) {

        Pattern p =
            Pattern.compile(
                "(?is)" +
                Pattern.quote(label) +
                "\\s*</td>\\s*" +
                "<td[^>]*>(.*?)</td>\\s*" +
                "<td[^>]*>(.*?)</td>"
            );

        Matcher m = p.matcher(table);

        if (!m.find()) {
            throw new IllegalStateException(
                "Ligne " + label + " introuvable"
            );
        }

        return new String[] {
            clean(m.group(1)),
            clean(m.group(2))
        };
    }

    private static String clean(String s) {

        return s
            .replaceAll("(?is)<[^>]+>", "")
            .replace("&nbsp;", " ")
            .trim();
    }

    private static String normalizeTime(
        String time
    ) {

        time =
            time.toLowerCase()
                .replace('h', ':')
                .trim();

        String[] parts =
            time.split(":");

        if (parts.length == 2) {

            return String.format(
                "%02d:%02d",
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1])
            );
        }

        return time;
    }
}