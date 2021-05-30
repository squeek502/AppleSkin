package squeek.appleskin.api.food;

public class FoodValues
{
    public final int hunger;
    public final float saturationModifier;

    public FoodValues(int hunger, float saturationModifier)
    {
        this.hunger = hunger;
        this.saturationModifier = saturationModifier;
    }

    public float getSaturationIncrement()
    {
        return hunger * saturationModifier * 2f;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof FoodValues)) return false;

        FoodValues that = (FoodValues) o;

        return hunger == that.hunger && Float.compare(that.saturationModifier, saturationModifier) == 0;
    }

    @Override
    public int hashCode()
    {
        int result = hunger;
        result = 31 * result + (saturationModifier != +0.0f ? Float.floatToIntBits(saturationModifier) : 0);
        return result;
    }
}
