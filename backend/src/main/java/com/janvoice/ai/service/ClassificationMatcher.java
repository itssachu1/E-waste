package com.janvoice.ai.service;

import com.janvoice.ai.entity.MaterialMaster;
import java.text.Normalizer;
import java.util.List;

/** Fuzzy matcher: maps a raw Gemini answer onto a real MaterialMaster entry. */
public final class ClassificationMatcher {

    private ClassificationMatcher() {}

    public static MaterialMaster bestMatch(String answer, List<MaterialMaster> materials) {
        if (answer == null || materials == null || materials.isEmpty()) return null;
        String norm = norm(answer);
        if (norm.isEmpty() || norm.contains("no_match") || norm.contains("no match")
                || norm.contains("unknown") || norm.contains("cannot")) return null;
        MaterialMaster best = null;
        int bestScore = Integer.MAX_VALUE;
        for (MaterialMaster m : materials) {
            if (m == null || !m.isActive()) continue;
            int score = distance(norm, norm(nameOf(m)));
            int score2 = distance(norm, norm(deviceOf(m)));
            int s = Math.min(score, score2);
            if (s < bestScore) { bestScore = s; best = m; }
        }
        if (best == null) return null;
        String target = norm(nameOf(best));
        int allowed = Math.max(2, target.length() / 4);
        if (norm.equals(target) || norm.contains(target) || target.contains(norm)) return best;
        return bestScore <= allowed ? best : null;
    }

    private static String nameOf(MaterialMaster m) {
        return m.getCommonName() == null ? "" : m.getCommonName();
    }

    private static String deviceOf(MaterialMaster m) {
        return m.getDeviceType() == null ? "" : m.getDeviceType();
    }

    static String norm(String s) {
        String n = Normalizer.normalize(s.toLowerCase().trim(), Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", "");
        return n.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] cur = new int[b.length() + 1];
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            prev = cur;
        }
        return prev[b.length()];
    }
}
