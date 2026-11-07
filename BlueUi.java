package com.fakedispenser;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.List;

/** Tiny helper for the blue GUI theme. */
public class BlueUi {
	public static final int TITLE = 0xFFBFDBFE;
	public static final int TEXT = 0xFFE0F2FE;
	public static final int SUB = 0xFF93C5FD;
	public static final int STATUS = 0xFF7DD3FC;

	public static void background(DrawContext ctx, int w, int h) {
		ctx.fillGradient(0, 0, w, h, 0xFF071229, 0xFF15397A);
	}

	public static void panel(DrawContext ctx, int x1, int y1, int x2, int y2) {
		ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xFF60A5FA);
		ctx.fill(x1, y1, x2, y2, 0xE60B2150);
	}

	/** Repaints vanilla buttons in blue (call after super.render). */
	public static void paintButtons(DrawContext ctx, TextRenderer tr, List<ButtonWidget> buttons) {
		for (ButtonWidget b : buttons) {
			if (!b.visible) continue;
			int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();
			int fill = !b.active ? 0xFF1E293B : (b.isHovered() ? 0xFF3B82F6 : 0xFF1D4ED8);
			ctx.fill(x, y, x + w, y + h, 0xFF93C5FD);
			ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
			ctx.drawCenteredTextWithShadow(tr, b.getMessage(), x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
		}
	}
}
