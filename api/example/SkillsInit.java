package com.takoyaki3466.mojibattle.init;

import com.takoyaki3466.mojibattle.ExpansionMod;
import com.takoyaki3466.mojibattle.tree.ReturnToForestSkill;
import jp.takoa.mojibattle.registry.MojiBattleRegister;
import jp.takoa.mojibattle.registry.RegistryHandle;
import jp.takoa.mojibattle.registry.impl.SkillImpl;


public class SkillsInit {
    public static final MojiBattleRegister.Skill SKILLS = MojiBattleRegister.createSkill(ExpansionMod.MOD_ID);

    public static final RegistryHandle<SkillImpl> TREE = SKILLS.register("return_to_forest", ReturnToForestSkill.class);
}
