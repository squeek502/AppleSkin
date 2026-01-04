package squeek.appleskin.mixin.util;

import com.google.common.primitives.Ints;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.Annotations;

import java.util.Collection;
import java.util.ListIterator;

@InjectionPoint.AtCode(namespace = "appleskin", value = "APPLESKIN_IINC")
public class BeforeInc extends InjectionPoint
{
	private final int ordinal;
	private final Integer intValue;

	public BeforeInc(IMixinContext context, AnnotationNode node, String returnType)
	{
		super(Annotations.<String>getValue(node, "slice", ""), Specifier.DEFAULT, null);

		this.ordinal = Annotations.<Integer>getValue(node, "ordinal", -1);
		this.intValue = Annotations.<Integer>getValue(node, "intValue", (Integer) null);
	}

	public BeforeInc(InjectionPointData data)
	{
		super(data);

		this.ordinal = data.getOrdinal();
		this.intValue = Ints.tryParse(data.get("intValue", ""));
	}

	@Override
	public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes)
	{
		boolean found = false;

		ListIterator<AbstractInsnNode> iter = insns.iterator();
		for (int ordinal = 0; iter.hasNext(); )
		{
			AbstractInsnNode insn = iter.next();

			boolean matchesInsn = this.matchesIncInsn(insn);
			if (matchesInsn)
			{
				if (this.ordinal == -1 || this.ordinal == ordinal)
				{
					nodes.add(insn);
					found = true;
				}
				ordinal++;
			}
		}

		return found;
	}

	private boolean matchesIncInsn(AbstractInsnNode insn)
	{
		if (insn.getOpcode() != Opcodes.IINC)
		{
			return false;
		}

		int incr = ((IincInsnNode) insn).incr;
		return this.intValue.equals(incr);
	}

}

