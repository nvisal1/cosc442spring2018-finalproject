/**
 *  Copyright (C) 2002-2015   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.server.model;

import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.server.control.ChangeSet;


// TODO: Auto-generated Javadoc
/**
 * A type of session to handle monarch actions that require response.
 */
public class MonarchSession extends TransactionSession {

    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(MonarchSession.class.getName());

    /** The player whose monarch is active. */
    private final ServerPlayer serverPlayer;

    /** The action to be considered. */
    private final MonarchAction action;

    /** The amount of tax to raise. */
    private final int tax;

    /** The goods for the goods party. */
    private Goods goods = null;

    /** Mercenaries on offer. */
    private List<AbstractUnit> mercenaries = null;

    /** Mercenary price. */
    private final int price;


    /**
     * Instantiates a new monarch session.
     *
     * @param serverPlayer the server player
     * @param action the action
     * @param tax the tax
     * @param goods the goods
     */
    public MonarchSession(ServerPlayer serverPlayer, MonarchAction action,
                          int tax, Goods goods) {
        super(makeSessionKey(MonarchSession.class, serverPlayer.getId(), ""));

        this.serverPlayer = serverPlayer;
        this.action = action;
        this.tax = tax;
        this.goods = goods;
        this.mercenaries = null;
        this.price = 0;
    }

    /**
     * Instantiates a new monarch session.
     *
     * @param serverPlayer the server player
     * @param action the action
     * @param mercenaries the mercenaries
     * @param price the price
     */
    public MonarchSession(ServerPlayer serverPlayer, MonarchAction action,
                          List<AbstractUnit> mercenaries, int price) {
        super(makeSessionKey(MonarchSession.class, serverPlayer.getId(), ""));

        this.serverPlayer = serverPlayer;
        this.action = action;
        this.tax = 0;
        this.goods = null;
        this.mercenaries = mercenaries;
        this.price = price;
    }

    /**
     * Complete.
     *
     * @param result the result
     * @param cs the cs
     */
    public void complete(boolean result, ChangeSet cs) {
        switch (action) {
        case RAISE_TAX_ACT: case RAISE_TAX_WAR:
            serverPlayer.csRaiseTax(tax, goods, result, cs);
            break;
        case MONARCH_MERCENARIES: case HESSIAN_MERCENARIES:
            if (result) serverPlayer.csAddMercenaries(mercenaries, price, cs);
            break;
        default:
            break;
        }
        super.complete(cs);
    }

    /* (non-Javadoc)
     * @see net.sf.freecol.server.model.TransactionSession#complete(net.sf.freecol.server.control.ChangeSet)
     */
    @Override
    public void complete(ChangeSet cs) {
        switch (action) {
        case RAISE_TAX_ACT: case RAISE_TAX_WAR:
            serverPlayer.ignoreTax(tax, goods, cs);
            break;
        case MONARCH_MERCENARIES: case HESSIAN_MERCENARIES:
            serverPlayer.ignoreMercenaries(cs);
            break;
        default:
            break;
        }
        super.complete(cs);
    }

    /**
     * Gets the action.
     *
     * @return the action
     */
    public MonarchAction getAction() {
        return this.action;
    }

    /**
     * Gets the tax.
     *
     * @return the tax
     */
    public int getTax() {
        return this.tax;
    }

    /**
     * Gets the goods.
     *
     * @return the goods
     */
    public Goods getGoods() {
        return this.goods;
    }

    /**
     * Gets the mercenaries.
     *
     * @return the mercenaries
     */
    public List<AbstractUnit> getMercenaries() {
        return this.mercenaries;
    }

    /**
     * Gets the price.
     *
     * @return the price
     */
    public int getPrice() {
        return this.price;
    }
}
