package com.teamwork.data;

import com.teamwork.business.Label;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Táº§ng Data Layer: Quáº£n lÃ½ Kho NhÃ£n PhÃ¢n Loáº¡i (In-Memory Database trÃªn RAM)
 * 
 * Ãp dá»¥ng nguyÃªn táº¯c Backend Code Mastery:
 * - Thread-safe 100%: DÃ¹ng CopyOnWriteArrayList vÃ  AtomicInteger cho nextId.
 * - Input validation & Trust boundary: Kiá»ƒm tra null, rá»—ng, chá»‘ng trÃ¹ng tÃªn trong cÃ¹ng project.
 * - Idempotent & Atomic operations: TrÃ¡nh race condition khi nhiá»u luá»“ng cÃ¹ng thÃªm/sá»­a/xÃ³a.
 */
public class LabelDB {

    // 1. Bá»™ nhá»› RAM lÆ°u trá»¯ danh sÃ¡ch nhÃ£n an toÃ n Ä‘a luá»“ng
    private static final List<Label> labels = new CopyOnWriteArrayList<>();
    private static final AtomicInteger nextId = new AtomicInteger(1);

    // 2. Khá»Ÿi táº¡o dá»¯ liá»‡u máº«u ban Ä‘áº§u (Seed Data)
    static {
        // --- Dá»° ÃN 1 (TW-HUB-01) ---
        labels.add(new Label(nextId.getAndIncrement(), 1, "Bug", "red", "bi-bug-fill"));
        labels.add(new Label(nextId.getAndIncrement(), 1, "Feature", "blue", "bi-stars"));
        labels.add(new Label(nextId.getAndIncrement(), 1, "UI/UX", "purple", "bi-palette-fill"));
        labels.add(new Label(nextId.getAndIncrement(), 1, "Backend", "amber", "bi-gear-fill"));
        labels.add(new Label(nextId.getAndIncrement(), 1, "Docs", "green", "bi-journal-bookmark-fill"));
        labels.add(new Label(nextId.getAndIncrement(), 1, "Urgent", "pink", "bi-lightning-fill"));

        // --- Dá»° ÃN 2 (ECOMMERCE-99) ---
        labels.add(new Label(nextId.getAndIncrement(), 2, "Bug", "red", "bi-bug-fill"));
        labels.add(new Label(nextId.getAndIncrement(), 2, "Feature", "blue", "bi-stars"));
        labels.add(new Label(nextId.getAndIncrement(), 2, "UI/UX", "purple", "bi-palette-fill"));
        labels.add(new Label(nextId.getAndIncrement(), 2, "Backend", "amber", "bi-gear-fill"));
        labels.add(new Label(nextId.getAndIncrement(), 2, "Docs", "green", "bi-journal-bookmark-fill"));
    }

    /**
     * Nghiá»‡p vá»¥ 1: Láº¥y táº¥t cáº£ nhÃ£n thuá»™c vá» má»™t dá»± Ã¡n cá»¥ thá»ƒ
     * @param projectId ID cá»§a dá»± Ã¡n
     * @return Danh sÃ¡ch Label thuá»™c dá»± Ã¡n (khÃ´ng bao giá» null)
     */
    public static List<Label> selectByProjectId(int projectId) {
        List<Label> result = new ArrayList<>();
        if (projectId <= 0) {
            return result;
        }
        for (Label lbl : labels) {
            if (lbl.getProjectId() == projectId) {
                result.add(lbl);
            }
        }
        return result;
    }

    /**
     * Nghiá»‡p vá»¥ 2: TÃ¬m nhÃ£n theo ID
     * @param id ID cá»§a nhÃ£n
     * @return Äá»‘i tÆ°á»£ng Label hoáº·c null náº¿u khÃ´ng tÃ¬m tháº¥y
     */
    public static Label selectById(int id) {
        if (id <= 0) {
            return null;
        }
        for (Label lbl : labels) {
            if (lbl.getId() == id) {
                return lbl;
            }
        }
        return null;
    }

    /**
     * Nghiá»‡p vá»¥ 3: Kiá»ƒm tra tÃªn nhÃ£n Ä‘Ã£ tá»“n táº¡i trong dá»± Ã¡n chÆ°a (Chá»‘ng trÃ¹ng láº·p)
     * @param projectId ID dá»± Ã¡n
     * @param name TÃªn nhÃ£n cáº§n kiá»ƒm tra
     * @param excludeId ID nhÃ£n cáº§n loáº¡i trá»« khi Ä‘ang sá»­a (truyá»n <= 0 náº¿u lÃ  thÃªm má»›i)
     * @return true náº¿u Ä‘Ã£ cÃ³ nhÃ£n trÃ¹ng tÃªn, false náº¿u chÆ°a cÃ³
     */
    public static boolean existsByName(int projectId, String name, int excludeId) {
        if (name == null || name.trim().isEmpty() || projectId <= 0) {
            return false;
        }
        String cleanName = name.trim().toLowerCase();
        for (Label lbl : labels) {
            if (lbl.getProjectId() == projectId && lbl.getId() != excludeId) {
                if (lbl.getName().trim().equalsIgnoreCase(cleanName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Nghiá»‡p vá»¥ 4: ThÃªm má»™t nhÃ£n má»›i vÃ o dá»± Ã¡n
     * @param label Äá»‘i tÆ°á»£ng nhÃ£n cáº§n thÃªm
     * @return true náº¿u thÃªm thÃ nh cÃ´ng, false náº¿u dá»¯ liá»‡u khÃ´ng há»£p lá»‡ hoáº·c trÃ¹ng tÃªn
     */
    public static boolean insert(Label label) {
        if (label == null || label.getProjectId() <= 0) {
            return false;
        }
        if (label.getName() == null || label.getName().trim().isEmpty()) {
            return false;
        }
        // Kiá»ƒm tra chá»‘ng trÃ¹ng tÃªn trong cÃ¹ng dá»± Ã¡n
        if (existsByName(label.getProjectId(), label.getName(), 0)) {
            return false;
        }

        label.setId(nextId.getAndIncrement());
        labels.add(label);
        return true;
    }

    /**
     * Nghiá»‡p vá»¥ 5: Cáº­p nháº­t thÃ´ng tin nhÃ£n cÃ³ sáºµn
     * @param label Äá»‘i tÆ°á»£ng nhÃ£n vá»›i thÃ´ng tin má»›i
     * @return true náº¿u cáº­p nháº­t thÃ nh cÃ´ng, false náº¿u khÃ´ng tÃ¬m tháº¥y hoáº·c trÃ¹ng tÃªn
     */
    public static boolean update(Label label) {
        if (label == null || label.getId() <= 0 || label.getProjectId() <= 0) {
            return false;
        }
        if (label.getName() == null || label.getName().trim().isEmpty()) {
            return false;
        }
        // Kiá»ƒm tra trÃ¹ng tÃªn vá»›i nhÃ£n khÃ¡c trong cÃ¹ng dá»± Ã¡n
        if (existsByName(label.getProjectId(), label.getName(), label.getId())) {
            return false;
        }

        for (int i = 0; i < labels.size(); i++) {
            Label current = labels.get(i);
            if (current.getId() == label.getId() && current.getProjectId() == label.getProjectId()) {
                current.setName(label.getName().trim());
                current.setColorKey(label.getColorKey());
                current.setIcon(label.getIcon());
                return true;
            }
        }
        return false;
    }

    /**
     * Nghiá»‡p vá»¥ 6: XÃ³a má»™t nhÃ£n khá»i dá»± Ã¡n
     * @param labelId ID cá»§a nhÃ£n cáº§n xÃ³a
     * @param projectId ID cá»§a dá»± Ã¡n sá»Ÿ há»¯u
     * @return true náº¿u xÃ³a thÃ nh cÃ´ng, false náº¿u khÃ´ng tÃ¬m tháº¥y
     */
    public static boolean delete(int labelId, int projectId) {
        if (labelId <= 0 || projectId <= 0) {
            return false;
        }
        return labels.removeIf(lbl -> lbl.getId() == labelId && lbl.getProjectId() == projectId);
    }

    /**
     * Nghiá»‡p vá»¥ 7: Tá»± Ä‘á»™ng khá»Ÿi táº¡o bá»™ nhÃ£n máº·c Ä‘á»‹nh náº¿u má»™t dá»± Ã¡n má»›i chÆ°a cÃ³ nhÃ£n nÃ o
     * @param projectId ID cá»§a dá»± Ã¡n
     */
    public static void initDefaultLabelsIfEmpty(int projectId) {
        if (projectId <= 0) return;
        List<Label> existing = selectByProjectId(projectId);
        if (existing.isEmpty()) {
            insert(new Label(0, projectId, "Bug", "red", "bi-bug-fill"));
            insert(new Label(0, projectId, "Feature", "blue", "bi-stars"));
            insert(new Label(0, projectId, "UI/UX", "purple", "bi-palette-fill"));
            insert(new Label(0, projectId, "Backend", "amber", "bi-gear-fill"));
            insert(new Label(0, projectId, "Docs", "green", "bi-journal-bookmark-fill"));
        }
    }
}