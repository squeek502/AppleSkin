package squeek.appleskin.api.handler;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface EventHandler<IEvent>
{
    static <T> Event<EventHandler<T>> createArrayBacked()
    {
        return EventFactory.createArrayBacked(EventHandler.class, listeners -> event -> {
            for (EventHandler listener : listeners) {
                listener.interact(event);
            }
        });
    }

    void interact(IEvent event);
}
