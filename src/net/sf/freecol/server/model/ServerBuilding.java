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
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;


// TODO: Auto-generated Javadoc
/**
 * The server version of a building.
 */
public class ServerBuilding extends Building implements ServerModelObject {

    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(ServerBuilding.class.getName());


    /**
     * Trivial constructor required for all ServerModelObjects.
     *
     * @param game the game
     * @param id the id
     */
    public ServerBuilding(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerBuilding.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param colony The <code>Colony</code> in which this building is located.
     * @param type The <code>BuildingType</code> of building.
     */
    public ServerBuilding(Game game, Colony colony, BuildingType type) {
        super(game, colony, type);
    }


    /**
     * New turn for this building.
     *
     * @param random A <code>Random</code> number source.
     * @param lb A <code>LogBuilder</code> to log to.
     * @param cs A <code>ChangeSet</code> to update.
     */
    @Override
    public void csNewTurn(Random random, LogBuilder lb, ChangeSet cs) {
        BuildingType type = getType();

        if (canTeach()) csTeach(cs);

        if (type.hasAbility(Ability.REPAIR_UNITS)) {
            csRepairUnits(cs);
        }
    }

    /**
     * Teach all the units in this school.
     *
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csTeach(ChangeSet cs) {
        final ServerPlayer owner = (ServerPlayer)getColony().getOwner();
        
        for (Unit teacher : getUnitList()) {
            Unit student = teacher.getStudent();

            if (student != null && student.getTeacher() != teacher) {
                // Sanitation, make sure we have the proper
                // teacher/student relation.
                student = relation(teacher);
            }

            // Student may have changed
            student = checkStudentChanged(cs, teacher, student);

            // Update teaching amount.
            updateTeacherAmount(cs, owner, teacher, student);

            // Do not check for completed training, see csCheckTeach below.
        }
    }

	private void updateTeacherAmount(ChangeSet cs, final ServerPlayer owner, Unit teacher, Unit student) {
		teacher.setTurnsOfTraining((student == null) ? 0
		    : teacher.getTurnsOfTraining() + 1);
		cs.add(See.only(owner), teacher);
	}

	private Unit checkStudentChanged(ChangeSet cs, Unit teacher, Unit student) {
		if (student == null && csAssignStudent(teacher, cs)) {
		    student = teacher.getStudent();
		}
		return student;
	}

	private Unit relation(Unit teacher) {
		Unit student;
		logger.warning("Bogus teacher/student assignment.");
		teacher.setStudent(null);
		student = null;
		return student;
	}

    /**
     * Check and complete teaching if possible.
     *
     * This needs to be separate and public because of the recheck of
     * teaching required if the colony production bonus rises at end
     * of new turn calculations.
     *
     * @param teacher The teaching <code>Unit</code>.
     * @param cs A <code>ChangeSet</code> to update.
     * @return True if teaching occurred.
     */
    public boolean csCheckTeach(Unit teacher, ChangeSet cs) {
        final ServerPlayer owner = (ServerPlayer)getColony().getOwner();

        return checkTeacherTraining(teacher, cs, owner);
    }

	private boolean checkTeacherTraining(Unit teacher, ChangeSet cs, final ServerPlayer owner) {
		Unit student = teacher.getStudent();
        if (student != null
            && teacher.getTurnsOfTraining()
                >= teacher.getNeededTurnsOfTraining()) {
            csTrainStudent(teacher, student, cs);
            // Student will have changed, teacher already added in csTeach
            cs.add(See.only(owner), student);
            if (teacher.getStudent() == null) csAssignStudent(teacher, cs);
            return true;
        }
        return false;
	}
        
    /**
     * Train a student.
     *
     * @param teacher The teacher <code>Unit</code>.
     * @param student The student <code>Unit</code> to train.
     * @param cs A <code>ChangeSet</code> to update.
     * @return True if teaching occurred.
     */
    private boolean csTrainStudent(Unit teacher, Unit student, ChangeSet cs) {
        final ServerPlayer owner = (ServerPlayer)getColony().getOwner();
        StringTemplate oldName = student.getLabel();
        UnitType teach = teacher.getType().getSkillTaught();
        UnitType skill = Unit.getUnitTypeTeaching(teach, student.getType());
        return getStudentTrained(teacher, student, cs, owner, oldName, skill);
    }

	private boolean getStudentTrained(Unit teacher, Unit student, ChangeSet cs, final ServerPlayer owner,
			StringTemplate oldName, UnitType skill) {
		boolean ret = skill != null;
        if (skill == null) {
            logger.warning("Student " + student.getId()
                           + " can not learn from " + teacher.getId());
        } else {
            student.changeType(skill);//-vis: safe within colony
            StringTemplate newName = student.getLabel();
            cs.addMessage(See.only(owner),
                new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                                 "model.building.unitEducated",
                                 getColony(), this)
                    .addStringTemplate("%oldName%", oldName)
                    .addStringTemplate("%unit%", newName)
                    .addName("%colony%", getColony().getName()));
        }
        student.setTurnsOfTraining(0);
        student.setMovesLeft(0);
        teacher.setTurnsOfTraining(0);
        teacher.setMovesLeft(0);
        if (!student.canBeStudent(teacher)) {
            student.setTeacher(null);
            teacher.setStudent(null);
        }
        return ret;
	}

    /**
     * Assigns a student to a teacher within a building.
     *
     * @param teacher The <code>Unit</code> that is teaching.
     * @param cs A <code>ChangeSet</code> to update.
     * @return True if a student was assigned.
     */
    private boolean csAssignStudent(Unit teacher, ChangeSet cs) {
        final Colony colony = getColony();
        final ServerPlayer owner = (ServerPlayer)colony.getOwner();
        final Unit student = colony.findStudent(teacher);
        return getStudentAssignment(teacher, cs, colony, owner, student);
    }

	private boolean getStudentAssignment(Unit teacher, ChangeSet cs, final Colony colony, final ServerPlayer owner,
			final Unit student) {
		if (student == null) {
            cs.addMessage(See.only(owner),
                new ModelMessage(ModelMessage.MessageType.WARNING,
                                 "model.building.noStudent",
                                 colony, teacher)
                          .addStringTemplate("%teacher%", teacher.getLabel())
                          .addName("%colony%", colony.getName()));
            return false;
        }
        teacher.setStudent(student);
        teacher.changeWorkType(null);
        student.setTeacher(teacher);
        cs.add(See.only(owner), student);
        return true;
	}

    /**
     * Repair the units in this building.
     *
     * @param cs A <code>ChangeSet</code> to update.
     */
    private void csRepairUnits(ChangeSet cs) {
        for (Unit unit : getTile().getUnitList()) {
            if (unit.isDamaged()
                && getType().hasAbility(Ability.REPAIR_UNITS,
                                        unit.getType())) {
                ((ServerUnit) unit).csRepairUnit(cs);
            }
        }
    }

    /**
     * Check a building to see if it is missing input.
     *
     * The building must need input, have a person working there, and have
     * no production occurring.
     *
     * @param pi The <code>ProductionInfo</code> for the building.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csCheckMissingInput(ProductionInfo pi, ChangeSet cs) {
        List<AbstractGoods> inputs = getInputs();
        if (!(inputs.isEmpty()
              || isEmpty()
              || canAutoProduce())
            && pi.getProduction().isEmpty()) {
            for (AbstractGoods goods : inputs) {
                cs.addMessage(See.only((ServerPlayer)getOwner()),
                    new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                     "model.building.notEnoughInput",
                                     this, goods.getType())
                        .addNamed("%inputGoods%", goods.getType())
                        .addNamed("%building%", this)
                        .addName("%colony%", getColony().getName()));
            }
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverBuilding"
     */
    @Override
    public String getServerXMLElementTagName() {
        return "serverBuilding";
    }
}
