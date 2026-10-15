package com.civic.complaint;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight "AI" classifier that runs fully offline.
 * It scores each category by the keywords found in the title (counted twice) and description,
 * and estimates priority from words that signal danger or urgency.
 * Swap {@link #classify} for a call to an ML model or LLM API later if you want.
 */
@Service
public class AiCategoryService {

    public record Result(String category, String priority, double confidence, List<String> matchedKeywords) {
    }

    public static final String OTHER = "Other";

    private static final Map<String, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put("Roads & Potholes", List.of("pothole", "potholes", "road", "roads", "crack", "cracked", "asphalt",
                "tar", "speed breaker", "footpath", "pavement", "sidewalk", "highway", "lane", "bridge", "divider"));
        KEYWORDS.put("Garbage & Sanitation", List.of("garbage", "trash", "waste", "dump", "dumping", "litter", "rubbish",
                "bin", "dustbin", "smell", "stink", "stinking", "sanitation", "dead animal", "debris", "plastic", "toilet"));
        KEYWORDS.put("Water Supply", List.of("water supply", "no water", "drinking water", "pipeline", "pipe", "leak",
                "leakage", "leaking", "tap", "contaminated", "dirty water", "burst", "low pressure", "water tank"));
        KEYWORDS.put("Drainage & Sewage", List.of("drain", "drainage", "sewage", "sewer", "overflow", "overflowing",
                "manhole", "clogged", "blocked drain", "waterlogging", "waterlogged", "stagnant", "flooded", "flooding", "gutter"));
        KEYWORDS.put("Streetlights", List.of("streetlight", "streetlights", "street light", "street lights", "lamp",
                "lamp post", "light pole", "dark street", "bulb", "lights not working", "light not working"));
        KEYWORDS.put("Electricity", List.of("electricity", "power cut", "power outage", "outage", "transformer", "electric",
                "live wire", "wire", "wires", "cable", "voltage", "sparking", "shock", "electric pole"));
        KEYWORDS.put("Parks & Trees", List.of("tree", "trees", "fallen tree", "branch", "branches", "park", "garden",
                "playground", "grass", "bench", "pruning"));
        KEYWORDS.put("Public Safety & Nuisance", List.of("stray dog", "stray dogs", "stray", "encroachment", "illegal",
                "noise", "accident", "unsafe", "harassment", "traffic signal", "signal", "abandoned vehicle"));
    }

    private static final List<String> HIGH = List.of("danger", "dangerous", "accident", "injury", "injured", "urgent",
            "emergency", "fire", "electrocution", "shock", "collapsed", "collapse", "live wire", "sparking", "burst",
            "flood", "flooding", "child", "children", "school", "hospital", "death", "fallen tree", "open manhole");

    private static final List<String> MEDIUM = List.of("overflow", "overflowing", "leak", "leaking", "broken", "blocked",
            "not working", "smell", "stink", "week", "weeks", "days", "dark", "traffic", "contaminated", "clogged");

    private final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

    public List<String> categories() {
        List<String> list = new ArrayList<>(KEYWORDS.keySet());
        list.add(OTHER);
        return list;
    }

    public Result classify(String title, String description) {
        String t = title == null ? "" : title;
        String d = description == null ? "" : description;
        String text = (t + " " + t + " " + d).toLowerCase(Locale.ROOT);

        String best = OTHER;
        int bestScore = 0;
        int total = 0;
        List<String> bestMatches = List.of();
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            List<String> matches = new ArrayList<>();
            int score = 0;
            for (String keyword : entry.getValue()) {
                int n = count(text, keyword);
                if (n > 0) {
                    matches.add(keyword);
                    score += n * (keyword.contains(" ") ? 2 : 1);
                }
            }
            total += score;
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
                bestMatches = matches;
            }
        }
        double confidence;
        if (bestScore == 0) {
            confidence = 0.3;
        } else {
            double share = bestScore / (double) total;
            confidence = Math.min(0.97, 0.45 + 0.35 * share + 0.05 * Math.min(bestScore, 4));
        }
        confidence = Math.round(confidence * 100) / 100.0;
        return new Result(best, priority(text), confidence, new ArrayList<>(new LinkedHashSet<>(bestMatches)));
    }

    String priority(String text) {
        if (HIGH.stream().anyMatch(k -> count(text, k) > 0)) {
            return "HIGH";
        }
        if (MEDIUM.stream().anyMatch(k -> count(text, k) > 0)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private int count(String text, String keyword) {
        Pattern pattern = patterns.computeIfAbsent(keyword, k -> Pattern.compile("\\b" + Pattern.quote(k) + "\\b"));
        Matcher matcher = pattern.matcher(text);
        int n = 0;
        while (matcher.find()) {
            n++;
        }
        return n;
    }
}
