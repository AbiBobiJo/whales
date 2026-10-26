package studio.interpretation.whales.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Блочный кит по эскизу: большая прямоугольная голова, тело, хвост
 * с горизонтальным плавником, два грудных плавника и ракушки на спине.
 * Текстура 256x64.
 */
public class WhaleModel extends EntityModel<WhaleRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart tail;
	private final ModelPart fluke;
	private final ModelPart rightFin;
	private final ModelPart leftFin;
	private final ModelPart[] barnacles;

	public WhaleModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = this.body.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.tail = this.body.getChild("tail");
		this.fluke = this.tail.getChild("fluke");
		this.rightFin = this.body.getChild("right_fin");
		this.leftFin = this.body.getChild("left_fin");
		this.barnacles = new ModelPart[] {
				this.head.getChild("barnacle_0"),
				this.head.getChild("barnacle_1"),
				this.body.getChild("barnacle_2"),
				this.tail.getChild("barnacle_3")
		};
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(80, 0).addBox(-9.0F, -8.0F, -9.0F, 18, 16, 18),
				PartPose.offset(0.0F, 14.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -9.0F, -20.0F, 20, 16, 20),
				PartPose.offset(0.0F, 0.0F, -9.0F));

		head.addOrReplaceChild("jaw",
				CubeListBuilder.create().texOffs(120, 40).addBox(-7.0F, 0.0F, -16.0F, 14, 3, 16),
				PartPose.offset(0.0F, 7.0F, -2.0F));

		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(152, 0).addBox(-5.0F, -5.0F, 0.0F, 10, 10, 16),
				PartPose.offset(0.0F, -1.0F, 9.0F));

		tail.addOrReplaceChild("fluke",
				CubeListBuilder.create().texOffs(0, 40).addBox(-14.0F, -1.0F, 0.0F, 28, 2, 10),
				PartPose.offset(0.0F, 0.0F, 15.0F));

		body.addOrReplaceChild("right_fin",
				CubeListBuilder.create().texOffs(80, 40).addBox(-10.0F, -1.0F, -4.0F, 10, 2, 8),
				PartPose.offsetAndRotation(-9.0F, 5.0F, -4.0F, 0.0F, 0.0F, -0.5F));

		body.addOrReplaceChild("left_fin",
				CubeListBuilder.create().texOffs(80, 40).mirror().addBox(0.0F, -1.0F, -4.0F, 10, 2, 8),
				PartPose.offsetAndRotation(9.0F, 5.0F, -4.0F, 0.0F, 0.0F, 0.5F));

		// Ракушки (видимость зависит от их количества у кита)
		CubeListBuilder barnacle = CubeListBuilder.create().texOffs(200, 40).addBox(-2.0F, -2.0F, -2.0F, 4, 2, 4);
		head.addOrReplaceChild("barnacle_0", barnacle, PartPose.offset(-4.0F, -9.0F, -14.0F));
		head.addOrReplaceChild("barnacle_1", barnacle, PartPose.offset(5.0F, -9.0F, -7.0F));
		body.addOrReplaceChild("barnacle_2", barnacle, PartPose.offset(-3.0F, -8.0F, 3.0F));
		tail.addOrReplaceChild("barnacle_3", barnacle, PartPose.offset(2.0F, -5.0F, 7.0F));

		return LayerDefinition.create(mesh, 256, 64);
	}

	@Override
	public void setupAnim(WhaleRenderState state) {
		super.setupAnim(state);

		float t = state.ageInTicks;
		float swim = state.inWater ? 1.0F : 0.35F;

		// Наклон всего тела по направлению взгляда (как у дельфина)
		this.body.xRot = state.xRot * Mth.DEG_TO_RAD;

		// Медленные взмахи хвостом вверх-вниз
		this.tail.xRot = Mth.sin(t * 0.08F) * 0.18F * swim;
		this.fluke.xRot = Mth.sin(t * 0.08F - 0.9F) * 0.32F * swim;

		// Плавники плавно гребут
		float fin = Mth.sin(t * 0.06F) * 0.15F;
		this.rightFin.zRot = -0.5F + fin;
		this.leftFin.zRot = 0.5F - fin;

		// Иногда приоткрывает рот
		this.jaw.xRot = Math.max(0.0F, Mth.sin(t * 0.02F)) * 0.12F;

		for (int i = 0; i < this.barnacles.length; i++) {
			this.barnacles[i].visible = i < state.barnacles;
		}
	}
}
