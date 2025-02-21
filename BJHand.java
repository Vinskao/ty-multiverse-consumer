import java.util.ArrayList;
import java.util.List;

public class BJHand {
    private List<BJCard> cards;
    public BJHand(){
        this.cards = new ArrayList<>();
    }

    public void addCard(BJCard card){
        cards.add(card);
    }

    public int getHandVal(){
        int value = 0;
        int aceCount = 0;
        for(BJCard card:cards){
            value += card.getValue();
            if("Ace".equals(card.getRank())){
                aceCount++;
            }
        }

        while(value > 21 && aceCount > 0){
            value -= 10;
            aceCount--;
        }

        return value;
    }

    public boolean isBlackJack(){
        return cards.size() == 2 && getHandVal() ==21;

    }

    public boolean isBust(){
        return getHandVal() > 21;
    }
    
    @Override
    public String toString(){
        return cards.toString() + " (Value: " + getHandVal() + ")";
    }
}
