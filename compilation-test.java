// Simple compilation test
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Constraint;
import uet.ndh.ddsl.core.building.ConstraintType;
import uet.ndh.ddsl.core.building.Visibility;
import uet.ndh.ddsl.core.JavaType;

public class CompilationTest {
    public static void testConstraintCopy() {
        Constraint constraint = new Constraint(ConstraintType.NOT_NULL, "", "");
        Constraint copy = constraint.copy();
        System.out.println("Constraint copy works");
    }

    public static void testFieldCopy() {
        Field field = new Field("test", new JavaType("String", ""), Visibility.PRIVATE, true, false, null);
        Field copy = field.copy();
        System.out.println("Field copy works");
    }

    public static void main(String[] args) {
        testConstraintCopy();
        testFieldCopy();
    }
}
