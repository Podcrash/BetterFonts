package betterfonts;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

public interface BetterFontConditionalClauses
{
    interface ReturnDiffType<IN, OUT>
    {
        OUT when(boolean condition, Function<IN, OUT> action, Function<IN, OUT> or);

        default OUT when(BooleanSupplier condition, Function<IN, OUT> action, Function<IN, OUT> or)
        {
            return when(condition.getAsBoolean(), action, or);
        }
    }

    interface ReturnSameType<T> extends ReturnDiffType<T, T>
    {
        T when(boolean condition, Function<T, T> action);

        default T when(BooleanSupplier condition, Function<T, T> action)
        {
            return when(condition.getAsBoolean(), action);
        }
    }
}
