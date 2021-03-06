/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jtd.entities;

import java.util.LinkedList;
import jtd.GameCtrl;
import jtd.PointD;
import jtd.PointI;
import jtd.def.MobDef;
import jtd.effect.instant.InstantEffect;
import jtd.effect.timed.TimedEffect;
import jtd.level.PathingGraph;

/**
 *
 * @author LostMekka
 */
public class Mob extends AnimatedEntity {

	public static final double WALK_RANDOM_COMPONENT = 0.33f;
	
	public MobDef def;
	public double speedMultiplier = 1f, armorOffset = 0f, bonusDamage = 0f;
	public double hp, shield;
	public boolean lastDamageWasSplashDamage = false;
	
	private double[] dmgCounters;
	private PathingGraph.PathingGraphIterator path = null;
	private PointD pathTarget;
	private LinkedList<TimedEffect> effects = new LinkedList<>(), effectsToRemove = new LinkedList<>();
	
	public Mob(PointD loc, MobDef mobDef){
		super(loc);
		def = mobDef;
		hp = mobDef.maxHP;
		shield = mobDef.maxShield;
		if(loc == null){
			path = GameCtrl.get().getCurrentPathingGraph(def.size).iterator();
			this.loc = path.getLastPoint().getPointD(def.size, WALK_RANDOM_COMPONENT);
			setAnimationSet(def.bithAnimations, false);
		} else {
			path = GameCtrl.get().getCurrentPathingGraph(def.size).iterator(getPointI());
			setAnimationSet(def.walkAnimations, true);
		}
		nextPathTarget();
		dmgCounters = new double[def.hitParticleFacts.length];
	}
	
	public Mob(MobDef mobDef){
		this(null, mobDef);
	}

	@Override
	public int getSize() {
		return def.size;
	}
	
	private void nextPathTarget(){
		PointI p = path.next();
		if(p == null){
			pathTarget = null;
		} else {
			pathTarget = p.getPointD(def.size, WALK_RANDOM_COMPONENT);
		}
		updateRotation();
	}
	
	private void updateRotation(){
		if(pathTarget == null){
			rotation = RANDOM.nextFloat() + 360f;
		} else {
			rotation = this.loc.getRotationTo(pathTarget);
		}
	}
	
	public final void updatePath(boolean aquireTargetImeediately) {
		if ((path == null) || (pathTarget == null)) {
			path = GameCtrl.get().getCurrentPathingGraph(def.size).iterator();
			nextPathTarget();
		} else {
			PointI p1 = path.getLastPoint();
			if (aquireTargetImeediately) {
				path = GameCtrl.get().getCurrentPathingGraph(def.size).iterator(getPointI());
				if (path != null) {
					PointI p2 = path.next();
					if ((p2 != null) && !p2.equals(p1)) {
						pathTarget = p2.getPointD(def.size, WALK_RANDOM_COMPONENT);
						updateRotation();
					}
				}
			} else {
				path = GameCtrl.get().getCurrentPathingGraph(def.size).iterator(pathTarget.getPointI(def.size));
			}
		}

	}
	
	public boolean walksIntoTower(Tower t) {
		int ms = def.size;
		int ts = t.def.size;
		PointI mp = pathTarget.getPointI(ms);
		PointI tp = t.getPointI();
		return ((mp.x + ms - 1 >= tp.x)
				&& (mp.x <= tp.x + ts - 1)
				&& (mp.y + ms - 1 >= tp.y)
				&& (mp.y <= tp.y + ts - 1));
	}

	public double getDistanceStillToWalk(){
		if(pathTarget == null) return 0f;
		return loc.distanceTo(pathTarget) + path.getDistanceLeft();
	}
	
	public void applyInstantEffect(InstantEffect effect){
		effect.apply(this);
	}
	
	public void applyTimedEffect(TimedEffect effect){
		effects.add(effect);
		effect.apply(this);
	}
	
	public void removeTimedEffect(TimedEffect effect){
		effectsToRemove.add(effect);
		effect.remove(this);
	}
	
	public double damage(double damage, Entity attacker){
		return damage(damage, attacker, null);
	}
	
	public double damage(double damage, Entity attacker, Double direction){
		damage += bonusDamage;
		if(damage <= shield){
			shield -= damage;
			return 0f;
		} else {
			damage -= shield;
			shield = 0;
			damage -= def.armor + armorOffset;
			if(damage <= 0) return 0f;
			hp -= damage;
			if(direction != null){
				// add hit particles
				for(int i=0; i<def.hitParticleFacts.length; i++){
					dmgCounters[i] += damage;
					while(dmgCounters[i] >= def.damagePerParticle[i]){
						dmgCounters[i] -= def.damagePerParticle[i];
						GameCtrl.get().addParticle(def.hitParticleFacts[i], loc.clone(), direction);
					}
				}
			}
			if(hp <= 0){
				// add death particles
				for(int i=0; i<def.deathParticleFacts.length; i++){
					for(int n=0; n<def.deathParticleCounts[i]; n++){
						GameCtrl.get().addParticle(def.deathParticleFacts[i], loc.clone(), rotation);
					}
				}
				// kill mob
				kill(attacker);
				return damage + hp;
			}
			return damage;
		}
	}

	@Override
	public void animatedEntityTick(double time) {
		// tick effects
		for(TimedEffect e:effects) e.tick(time, this);
		for(TimedEffect e:effectsToRemove) effects.remove(e);
		effectsToRemove.clear();
		// regen hp
		hp += time * def.hpRegen;
		if(hp > def.maxHP) hp = def.maxHP;
		if(hp <= 0) kill(null);
		// regen shield
		shield += time * def.shieldRegen;
		if(shield > def.maxShield) shield = def.maxShield;
		if(shield < 0) shield = 0;
		// move mob
		if(pathTarget != null){
			double travel = time * def.speed * speedMultiplier;
			while(travel > 0){
				travel -= loc.travelTo(pathTarget, travel, true);
				if(travel > 0){
					PointD lastTargetPoint = pathTarget;
					nextPathTarget();
					// if we have reached the last node in the path, wander around randomly
					if(pathTarget == null){
						GameCtrl.get().pathEndReachedBy(getPointI(), this);
						pathTarget = loc.clone();
						double d = RANDOM.nextFloat() * 1.5f + 0.5f;
						double a = RANDOM.nextFloat() * 360f;
						pathTarget.x += d * (double)Math.cos(a);
						pathTarget.y += d * (double)Math.sin(a);
						GameCtrl.get().movePointIntoLevel(pathTarget);
					} else {
						GameCtrl.get().fieldWalkedBy(getPointI(), this);
					}
					// set proper direction
					rotation = loc.getRotationTo(pathTarget);
				}
			}
		}
		// let the def tick (for special mobs only)
		def.defTick(this, time);
	}

}
