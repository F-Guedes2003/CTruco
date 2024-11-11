package com.fhelippe.ana.tiaodotruco;

import com.bueno.spi.model.CardToPlay;
import com.bueno.spi.model.GameIntel;
import com.bueno.spi.model.TrucoCard;
import com.bueno.spi.service.BotServiceProvider;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TiaoDoTruco implements BotServiceProvider {

    @Override
    public boolean getMaoDeOnzeResponse(GameIntel intel) {
        return handStrength(intel) > 27;
    }

    @Override
    public boolean decideIfRaises(GameIntel intel) {
        if (intel.getRoundResults().size() >= 1) {
            if (hasZap(intel) && hasCopas(intel)) return true;

            if (wonFirstRound(intel)) {
                if (hasManilha(intel)) return true;
                if (hasZap(intel)) return true;

            } else if (!wonFirstRound(intel)) {
                if (handStrength(intel) > 20 && hasManilha(intel)) return true;
            }

            if (intel.getRoundResults().size() > 1 && intel.getRoundResults().get(1) == GameIntel.RoundResult.WON && intel.getRoundResults().get(0) == GameIntel.RoundResult.LOST) {
                if (hasManilha(intel)) return true;
                if (handStrength(intel) > 20 && (hasZap(intel) || hasCopas(intel))) return true;
            }

            if (intel.getRoundResults().size() == 2 && intel.getOpponentCard().isPresent())
                if (canKill(intel, getStrongestCard(intel))) return true;
        }

        return handStrength(intel) > 27 && hasManilha(intel);
    }

    @Override
    public CardToPlay chooseCard(GameIntel intel) {

        int roundsPlayed = intel.getRoundResults().size();

        if (roundsPlayed == 0) return playFirstRound(intel);

        else if (roundsPlayed == 1) return playSecondRound(intel);

        else return CardToPlay.of(getStrongestCard(intel));
    }

    @Override
    public int getRaiseResponse(GameIntel intel) {
        if (hasZap(intel) && hasEspadilha(intel)) return 1;

        if (intel.getRoundResults().isEmpty()) {
            if (hasManilha(intel) && hasBiggerThanTwo(intel).size() > 1) return 1;

            if (hasBiggerThanTwo(intel).size() > 1) return 0;
        }

        if (intel.getRoundResults().size() == 1) {
            if (wonFirstRound(intel) && (hasManilha(intel) || hasBiggerThanTwo(intel).size() > 1)) return 1;
        }

        if (wonFirstRound(intel)) {
            if (intel.getOpponentCard().isPresent()) {
                if (canKill(intel, intel.getCards().get(0))) return 1;

                else return -1;
            }

            if (handStrength(intel) > 9) return 0;
        }

        return -1;
    }

    ///////////////////////////////////////////////
    // Non Required methods
    ///////////////////////////////////////////////

    private CardToPlay playFirstRound(GameIntel intel) {
        double strength = handStrength(intel);

        if (intel.getOpponentCard().isPresent()) {
            TrucoCard opponentCard = intel.getOpponentCard().get();

            Optional<TrucoCard> killingCard = intel.getCards().stream()
                    .filter(card -> card.compareValueTo(opponentCard, intel.getVira()) > 0)
                    .max((card1, card2) -> card1.compareValueTo(card2, intel.getVira()));

            if (killingCard.isPresent()) {
                return CardToPlay.of(killingCard.get());
            }

            return CardToPlay.of(getWeakestCard(intel));
        }

        if (strength > 25) return getMidCard(intel).map(CardToPlay::of).orElse(CardToPlay.of(getStrongestCard(intel)));

        if (strength > 14) return getMidCard(intel).map(CardToPlay::of).orElse(CardToPlay.of(getStrongestCardWithoutManilha(intel).get()));

        if (strength <= 14) {
            if (hasZap(intel) || hasCopas(intel)) {
                return CardToPlay.of(getWeakestCard(intel));
            }
            return CardToPlay.of(getWeakestCard(intel));
        }

        return CardToPlay.of(getMidCard(intel).get());
    }

    private CardToPlay playSecondRound(GameIntel intel) {
        boolean wonFirstRound = wonFirstRound(intel);
        double handStrength = handStrength(intel);

        if (intel.getOpponentCard().isPresent()) {
            TrucoCard opponentCard = intel.getOpponentCard().get();
            if (opponentCard.relativeValue(intel.getVira()) > 20) {
                Optional<TrucoCard> strongestCard = getStrongestCardWithoutManilha(intel);
                if (strongestCard.isPresent()) {
                    return CardToPlay.of(strongestCard.get());
                }
            }
        }

        if (wonFirstRound) return getMidCard(intel).map(CardToPlay::of).orElse(CardToPlay.of(getStrongestCard(intel)));

        if (handStrength > 14 || hasManilha(intel)) {
            return CardToPlay.of(getStrongestCard(intel));
        }

        return CardToPlay.of(getWeakestCard(intel));
    }



    private Set<TrucoCard> hasBiggerThanTwo(GameIntel intel) {
        return intel.getCards().stream()
                .filter(e -> e.getRank().value() > 9)
                .collect(Collectors.toSet());
    }

    public int countManilha(GameIntel intel) {
        return intel.getCards().stream()
                .filter(e -> e.isManilha(intel.getVira()))
                .collect(Collectors.toSet())
                .size();
    }

    public boolean hasManilha(GameIntel intel) {
        return intel.getCards()
                .stream()
                .anyMatch(e -> e.isManilha(intel.getVira()));
    }

    public boolean hasZap(GameIntel intel) {
        return intel.getCards()
                .stream()
                .anyMatch(e -> e.isZap(intel.getVira()));
    }

    public boolean hasCopas(GameIntel intel) {
        return intel.getCards()
                .stream()
                .anyMatch(e -> e.isCopas(intel.getVira()));
    }

    public boolean hasEspadilha(GameIntel intel) {
        return intel.getCards()
                .stream()
                .anyMatch(e -> e.isEspadilha(intel.getVira()));
    }

    public boolean hasOuros(GameIntel intel) {
        return intel.getCards()
                .stream()
                .anyMatch(e -> e.isOuros(intel.getVira()));
    }

    public double handStrength(GameIntel intel) {
        return intel.getCards().stream()
                .mapToDouble(e -> e.relativeValue(intel.getVira()))
                .sum();
    }

    private TrucoCard getManilhaCard(GameIntel intel) {
        return intel.getCards()
                .stream()
                .filter(card -> card.isManilha(intel.getVira()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No manilha card found"));
    }

    public TrucoCard getStrongestCard(GameIntel intel) {
        if (hasZap(intel)) return intel.getCards().stream()
                .filter(e -> e.isZap(intel.getVira()))
                .findFirst()
                .orElseThrow(() -> new NullPointerException("No such element"));

        if (hasCopas(intel)) return intel.getCards().stream()
                .filter(e -> e.isCopas(intel.getVira()))
                .findFirst()
                .orElseThrow(() -> new NullPointerException("No such element"));

        if (hasEspadilha(intel)) return intel.getCards().stream()
                .filter(e -> e.isEspadilha(intel.getVira()))
                .findFirst()
                .orElseThrow(() -> new NullPointerException("No such element"));

        if (hasOuros(intel)) return intel.getCards().stream()
                .filter(e -> e.isOuros(intel.getVira()))
                .findFirst()
                .orElseThrow(() -> new NullPointerException("No such element"));

        return intel.getCards().stream()
                .max((card1, card2) -> card1.compareValueTo(card2, intel.getVira()))
                .orElseThrow(() -> new NullPointerException("There is no Cards"));
    }

    public Optional<TrucoCard> getStrongestCardWithoutManilha(GameIntel intel) {
        return intel.getCards().stream()
                .filter(e -> !e.isManilha(intel.getVira()))
                .max((card1, card2) -> card1.compareValueTo(card2, intel.getVira()));
    }

    public TrucoCard getWeakestCard(GameIntel intel) {
        return intel.getCards().stream()
                .min((card1, card2) -> card1.compareValueTo(card2, intel.getVira()))
                .orElseThrow(() -> new NullPointerException("There is no Cards"));
    }

    private Optional<TrucoCard> getMidCard(GameIntel intel) {
        return intel.getCards().stream()
                .filter(e -> !e.equals(getStrongestCard(intel)) && !e.equals(getWeakestCard(intel)) )
                .findFirst();
    }


    public boolean wonFirstRound(GameIntel intel) {
        return !intel.getRoundResults().isEmpty() && intel.getRoundResults().get(0) == GameIntel.RoundResult.WON;
    }

    public boolean canKill(GameIntel intel, TrucoCard card) {
        if (intel.getOpponentCard().isPresent()) {
            int opponentCardValue = intel.getOpponentCard().get().relativeValue(intel.getVira());
            int cardValue = card.relativeValue(intel.getVira());
            return cardValue > opponentCardValue;
        }
        return false;
    }



    public boolean isZapAlreadyUsed(GameIntel intel) {
        Optional<TrucoCard> maybeZap= intel.getOpenCards()
                .stream()
                .filter(e -> e.isZap(intel.getVira()))
                .findFirst();

        return maybeZap.isPresent();
    }

}