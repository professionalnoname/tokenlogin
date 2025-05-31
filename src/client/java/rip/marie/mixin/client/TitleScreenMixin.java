package rip.marie.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rip.marie.Util;

@Mixin(
		TitleScreen.class
)
public abstract class TitleScreenMixin extends Screen
{
	/**
	 * Text field that the player can input the token in
	 */
	private TextFieldWidget tokenField;

	protected TitleScreenMixin(final Text title)
	{
		super(title);
	}

	@Inject(
			at = @At("TAIL"),
			method = "init"
	)
	private void init(final CallbackInfo info)
	{
		this.tokenField = new TextFieldWidget(this.textRenderer, 0, 0, 200, 20, Text.of("enter token here"));
		this.tokenField.setMaxLength(Short.MAX_VALUE); // should be enough LOL

		this.addSelectableChild(this.tokenField);
		ButtonWidget buttonWidget = this.addDrawableChild(ButtonWidget.builder(Text.of("login"), (button) -> Util.login(tokenField)).dimensions(0, 25, 200, 20).build());

		if (Util.UNSAFE == null)
		{
			Util.status = "Failed to initialize Unsafe access! Look into the game log for more info!";
			buttonWidget.active = false;
			this.tokenField.active = false;
		}
	}

	@Inject(
			at = @At("TAIL"),
			method = "render"
	)
	private void render(final DrawContext context,
						final int mouseX,
						final int mouseY,
						final float delta,
						final CallbackInfo info)
	{
		this.tokenField.render(context, mouseX, mouseY, delta);
		context.drawText(this.textRenderer, Util.status, 5, 50, -1, true);
	}
}