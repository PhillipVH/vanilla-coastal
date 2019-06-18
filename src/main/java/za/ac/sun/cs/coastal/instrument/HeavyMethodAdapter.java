package za.ac.sun.cs.coastal.instrument;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import za.ac.sun.cs.coastal.COASTAL;
import za.ac.sun.cs.coastal.Trigger;
import za.ac.sun.cs.coastal.diver.SymbolicState;

public class HeavyMethodAdapter extends MethodVisitor {

	private static final String SYMBOLIC = "za/ac/sun/cs/coastal/Symbolic";

	private static final String LIBRARY = "za/ac/sun/cs/coastal/symbolic/VM";

	private static final String VERIFIER = "org/sosy_lab/sv_benchmarks/Verifier";
	
	private final COASTAL coastal;

	private final Logger log;

	private final boolean useConcreteValues;

	private final InstrumentationClassManager classManager;

	private final int triggerIndex;

	private final boolean isStatic;

	private final int argCount;

	private static final class Tuple {
		final int min, max, cur;

		Tuple(int min, int max, int cur) {
			this.min = min;
			this.max = max;
			this.cur = cur;
		}
	}

	private static Map<Label, Stack<Tuple>> caseLabels = new HashMap<>();

	private static Set<Label> catchLabels = new HashSet<>();

	private BitSet currentLinenumbers;

	// private static BitSet currentBranchInstructions;

	public HeavyMethodAdapter(COASTAL coastal, MethodVisitor cv, int triggerIndex, boolean isStatic, int argCount) {
		super(Opcodes.ASM6, cv);
		this.coastal = coastal;
		this.log = coastal.getLog();
		this.useConcreteValues = coastal.getConfig().getBoolean("coastal.settings.concrete-values", false);
		this.classManager = coastal.getClassManager();
		this.triggerIndex = triggerIndex;
		this.isStatic = isStatic;
		this.argCount = argCount;
	}

	private int visitParameter(Trigger trigger, int triggerIndex, int index, int address) {
		Class<?> type = trigger.getParamType(index);
		int size = ((type == long.class) || (type == double.class)) ? 2 : 1;
		if (type == boolean.class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ILOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteBoolean", "(IIIZ)Z", false);
			mv.visitIntInsn(Opcodes.ISTORE, address);
		} else if (type == byte.class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ILOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteByte", "(IIIB)B", false);
			mv.visitIntInsn(Opcodes.ISTORE, address);
		} else if (type == short.class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ILOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteShort", "(IIIS)S", false);
			mv.visitIntInsn(Opcodes.ISTORE, address);
		} else if (type == char.class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ILOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteChar", "(IIIC)C", false);
			mv.visitIntInsn(Opcodes.ISTORE, address);
		} else if (type == int.class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ILOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteInt", "(IIII)I", false);
			mv.visitIntInsn(Opcodes.ISTORE, address);
		} else if (type == long.class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.LLOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteLong", "(IIIJ)J", false);
			mv.visitIntInsn(Opcodes.LSTORE, address);
		} else if (type == float.class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.FLOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteFloat", "(IIIF)F", false);
			mv.visitIntInsn(Opcodes.FSTORE, address);
		} else if (type == double.class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.DLOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteDouble", "(IIID)D", false);
			mv.visitIntInsn(Opcodes.DSTORE, address);
		} else if (type == String.class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ALOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteString",
					"(IIILjava/lang/String;)Ljava/lang/String;", false);
			mv.visitIntInsn(Opcodes.ASTORE, address);
		} else if (type == boolean[].class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ALOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteBooleanArray", "(III[Z)[Z", false);
			mv.visitIntInsn(Opcodes.ASTORE, address);
		} else if (type == byte[].class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ALOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteByteArray", "(III[B)[B", false);
			mv.visitIntInsn(Opcodes.ASTORE, address);
		} else if (type == short[].class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ALOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteShortArray", "(III[S)[S", false);
			mv.visitIntInsn(Opcodes.ASTORE, address);
		} else if (type == char[].class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ALOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteCharArray", "(III[C)[C", false);
			mv.visitIntInsn(Opcodes.ASTORE, address);
		} else if (type == int[].class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ALOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteIntArray", "(III[I)[I", false);
			mv.visitIntInsn(Opcodes.ASTORE, address);
		} else if (type == long[].class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ALOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteLongArray", "(III[J)[J", false);
			mv.visitIntInsn(Opcodes.ASTORE, address);
		} else if (type == float[].class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ALOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteFloatArray", "(III[F)[F", false);
			mv.visitIntInsn(Opcodes.ASTORE, address);
		} else if (type == double[].class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ALOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteDoubleArray", "(III[D)[D", false);
			mv.visitIntInsn(Opcodes.ASTORE, address);
		} else if (type == String[].class) {
			mv.visitLdcInsn(triggerIndex);
			mv.visitLdcInsn(index);
			mv.visitLdcInsn(address);
			mv.visitIntInsn(Opcodes.ALOAD, address);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getConcreteStringArray",
					"(III[Ljava/lang/String;)[Ljava/lang/String;", false);
			mv.visitIntInsn(Opcodes.ASTORE, address);
		} else {
			log.fatal("UNHANDLED PARAMETER TYPE");
			System.exit(1);
		}
		return size;
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		log.trace("visitLineNumber(line:{}, label:{})", line, start);
		mv.visitLdcInsn(classManager.getInstructionCounter());
		mv.visitLdcInsn(line);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "linenumber", "(II)V", false);
		mv.visitLineNumber(line, start);
		currentLinenumbers.set(line);
	}

	@Override
	public void visitEnd() {
		log.trace("visitEnd()");
		classManager.registerLastInstruction();
		classManager.registerLinenumbers(currentLinenumbers);
		// branchInstructions.put(methodCounter, currentBranchInstructions);
		mv.visitEnd();
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		log.trace("visitTryCatchBlock(start:{}, end:{}, handler:{}, type:{})", start, end, handler, type);
		catchLabels.add(handler);
		mv.visitTryCatchBlock(start, end, handler, type);
	}

	@Override
	public void visitCode() {
		log.trace("visitCode()");
		if (triggerIndex >= 0) {
			//--- IF (symbolicMode) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "getRecordingMode", "()Z", false);
			Label label = new Label();
			mv.visitJumpInsn(Opcodes.IFNE, label);
			//---   triggerMethod()
			mv.visitLdcInsn(classManager.getNextMethodCounter());
			mv.visitLdcInsn(triggerIndex);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "triggerMethod", "(II)V", false);
			//---   GENERATE PARAMETER OVERRIDES
			Trigger trigger = coastal.getTrigger(triggerIndex);
			int n = trigger.getParamCount();
			int offset = (isStatic ? 0 : 1);
			int address = offset;
			for (int i = 0; i < n; i++) {
				address += visitParameter(trigger, triggerIndex, i, address);
			}
			Label end = new Label();
			mv.visitJumpInsn(Opcodes.GOTO, end);
			mv.visitLabel(label);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			//--- } else {
			//---   startMethod()
			mv.visitLdcInsn(classManager.getMethodCounter());
			mv.visitLdcInsn(argCount + (isStatic ? 0 : 1));
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "startMethod", "(II)V", false);
			//--- }
			mv.visitLabel(end);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		} else {
			mv.visitLdcInsn(classManager.getNextMethodCounter());
			mv.visitLdcInsn(argCount + (isStatic ? 0 : 1));
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "startMethod", "(II)V", false);
		}
		classManager.registerFirstInstruction();
		currentLinenumbers = new BitSet();
		// currentBranchInstructions = new BitSet();
		mv.visitCode();
	}

	@Override
	public void visitInsn(int opcode) {
		log.trace("visitInsn(opcode:{})", opcode);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(opcode);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "insn", "(II)V", false);
		mv.visitInsn(opcode);
		if (opcode == Opcodes.IDIV) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "noException", "()V", false);
		}
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		log.trace("visitIntInsn(opcode:{}, operand:{})", opcode, operand);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(opcode);
		mv.visitLdcInsn(operand);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "intInsn", "(III)V", false);
		mv.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		log.trace("visitVarInsn(opcode:{}, var:{})", opcode, var);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(opcode);
		mv.visitLdcInsn(var);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "varInsn", "(III)V", false);
		mv.visitVarInsn(opcode, var);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		log.trace("visitTypeInsn(opcode:{}, type:{})", opcode, type);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(opcode);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "typeInsn", "(II)V", false);
		mv.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		log.trace("visitFieldInsn(opcode:{}, owner:{}, name:{})", opcode, owner, name);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(opcode);
		mv.visitLdcInsn(owner);
		mv.visitLdcInsn(name);
		mv.visitLdcInsn(descriptor);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "fieldInsn",
				"(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
		mv.visitFieldInsn(opcode, owner, name, descriptor);
	}

	private char primitiveReturnType(String descriptor) {
		String type = SymbolicState.getReturnType(descriptor);
		if (type.length() == 0) {
			return 'X';
		}
		switch (type.charAt(0)) {
		case 'B':
		case 'C':
		case 'D':
		case 'F':
		case 'I':
		case 'J':
		case 'S':
			return type.charAt(0);
		default:
			return 'X';
		}
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		log.trace("visitMethodInsn(opcode:{}, owner:{}, name:{})", opcode, owner, name);
		if (owner.equals(SYMBOLIC)) {
			mv.visitMethodInsn(opcode, LIBRARY, name, descriptor, isInterface);
			// pop params !!!!!!!!!!!
		} else {
			mv.visitLdcInsn(classManager.getNextInstructionCounter());
			mv.visitLdcInsn(opcode);
			mv.visitLdcInsn(owner);
			mv.visitLdcInsn(name);
			mv.visitLdcInsn(descriptor);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "methodInsn",
					"(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
			mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			if (owner.equals(VERIFIER)) {
				switch (name) {
				case "nondetInt":
					mv.visitLdcInsn(classManager.getNextNewVariableCounter());
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "createSymbolicInt", "(II)I", false);
					break;
				case "nondetShort":
					mv.visitLdcInsn(classManager.getNextNewVariableCounter());
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "createSymbolicShort", "(SI)S", false);
					break;
				case "nondetBoolean":
					mv.visitLdcInsn(classManager.getNextNewVariableCounter());
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "createSymbolicBoolean", "(ZI)Z", false);
					break;
				case "nondetByte":
					mv.visitLdcInsn(classManager.getNextNewVariableCounter());
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "createSymbolicByte", "(BI)B", false);
					break;
				case "nondetChar":
					mv.visitLdcInsn(classManager.getNextNewVariableCounter());
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "createSymbolicChar", "(CI)C", false);
					break;
				case "nondetLong":
					mv.visitLdcInsn(classManager.getNextNewVariableCounter());
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "createSymbolicLong", "(JI)J", false);
					break;
				case "nondetFloat":
					mv.visitLdcInsn(classManager.getNextNewVariableCounter());
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "createSymbolicFloat", "(FI)F", false);
					break;
				case "nondetDouble":
					mv.visitLdcInsn(classManager.getNextNewVariableCounter());
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "createSymbolicDouble", "(DI)D", false);
					break;
				case "nondetString":
				default:
					log.fatal("Unimplemented verifier method {}.{}", owner, name);
					System.exit(1);					
				}
			} else {
				String className = owner.replace('/', '.');
				if (useConcreteValues && !coastal.isTarget(className)
						&& (coastal.findDelegate(owner, className, descriptor) == null)) {
					char returnType = primitiveReturnType(descriptor);
					if (returnType != 'X') {
						mv.visitInsn(Opcodes.DUP);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "returnValue", "(" + returnType + ")V", false);
					}
				}
			}
		}
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
			Object... bootstrapMethodArguments) {
		log.trace("visitInvokeDynamicInsn(name:{})", name);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(186);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "invokeDynamicInsn", "(II)V", false);
		mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		log.trace("visitJumpInsn(opcode:{}, label:{})", opcode, label);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(opcode);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "jumpInsn", "(II)V", false);
		mv.visitJumpInsn(opcode, label);
		if (opcode != Opcodes.GOTO) {
			mv.visitLdcInsn(classManager.getInstructionCounter());
			mv.visitLdcInsn(opcode);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "postJumpInsn", "(II)V", false);
			// currentBranchInstructions.set(instructionCounter);
		}
	}

	@Override
	public void visitLdcInsn(Object value) {
		log.trace("visitLdcInsn(value:{})", value);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(18);
		mv.visitLdcInsn(value);
		if (value instanceof Integer) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "ldcInsn", "(III)V", false);
		} else if (value instanceof Long) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "ldcInsn", "(IIJ)V", false);
		} else if (value instanceof Float) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "ldcInsn", "(IIF)V", false);
		} else if (value instanceof Double) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "ldcInsn", "(IID)V", false);
		} else {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "ldcInsn", "(IILjava/lang/Object;)V", false);
		}
		mv.visitLdcInsn(value);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		log.trace("visitJumpInsn(var:{}, increment:{})", var, increment);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(var);
		mv.visitLdcInsn(increment);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "iincInsn", "(III)V", false);
		mv.visitIincInsn(var, increment);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		log.trace("visitTableSwitchInsn(min:{}, max:{}, dflt:{})", min, max, dflt);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(170);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "tableSwitchInsn", "(II)V", false);
		assert labels.length == (max - min + 1);
		for (int value = min; value <= max; value++) {
			Stack<Tuple> pending = caseLabels.get(labels[value - min]);
			if (pending == null) {
				pending = new Stack<>();
				caseLabels.put(labels[value - min], pending);
			}
			pending.push(new Tuple(min, max, value));
		}
		Stack<Tuple> pending = caseLabels.get(dflt);
		if (pending == null) {
			pending = new Stack<>();
			caseLabels.put(dflt, pending);
		}
		pending.push(new Tuple(min, max, min - 1));
		mv.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitLabel(Label label) {
		log.trace("visitLabel(label:{})", label);
		mv.visitLabel(label);
		if (catchLabels.contains(label)) {
			mv.visitLdcInsn(classManager.getInstructionCounter());
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "startCatch", "(I)V", false);
		} else {
			Stack<Tuple> pending = caseLabels.get(label);
			if (pending != null) {
				while (!pending.isEmpty()) {
					Tuple t = pending.pop();
					mv.visitLdcInsn(t.min);
					mv.visitLdcInsn(t.max);
					mv.visitLdcInsn(t.cur);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "tableCaseInsn", "(III)V", false);
				}
			}
		}
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		log.trace("visitLookupSwitchInsn(dflt:{})", dflt);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(171);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "lookupSwitchInsn", "(II)V", false);
		mv.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		log.trace("visitMultiANewArrayInsn(numDimensions:{})", numDimensions);
		mv.visitLdcInsn(classManager.getNextInstructionCounter());
		mv.visitLdcInsn(197);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY, "multiANewArrayInsn", "(II)V", false);
		mv.visitMultiANewArrayInsn(descriptor, numDimensions);
	}

}
