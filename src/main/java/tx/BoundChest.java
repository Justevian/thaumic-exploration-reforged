package tx;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.blockentity.TcBlockEntity;

final class BoundChest extends Block implements EntityBlock {
    BoundChest(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Tile chest) || chest.channel.isEmpty()) return InteractionResult.PASS;
        if (level instanceof ServerLevel) {
            player.openMenu(new SimpleMenuProvider((id, inventory, viewer) ->
                new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, id, inventory, chest.inventory(), 3) {
                    @Override
                    public boolean stillValid(Player player) {
                        return !chest.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64;
                    }
                }, Component.translatable("block.thaumicexploration.bound_chest")));
        }
        return InteractionResult.SUCCESS;
    }

    static final class Tile extends TcBlockEntity {
        private String channel = "";

        Tile(BlockPos pos, BlockState state) {
            super(Exploration.CHEST_TILE.get(), pos, state);
        }

        boolean linked() {
            return !channel.isEmpty();
        }

        SimpleContainer inventory() {
            return BoundStorage.inventory((ServerLevel) level, channel);
        }

        void bind(String channel) {
            this.channel = channel;
            sync();
            level.invalidateCapabilities(worldPosition);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            channel = input.getStringOr("channel", "");
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putString("channel", channel);
        }
    }

    static final class Seal extends Item {
        Seal(Properties properties) {
            super(properties.stacksTo(1));
        }

        @Override
        public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
            BlockPos pos = context.getClickedPos();
            Level level = context.getLevel();
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.CHEST) || state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) return InteractionResult.PASS;
            Player player = context.getPlayer();
            if (player == null) return InteractionResult.PASS;
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (!server.mayInteract(player, pos) || !(level.getBlockEntity(pos) instanceof ChestBlockEntity original)
                || !original.isEmpty()) return InteractionResult.FAIL;

            String previous = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr("channel", "");
            if (stack.is(Exploration.LINKED_SEAL.get()) && previous.isEmpty()) return InteractionResult.FAIL;
            String channel = previous.isEmpty() ? UUID.randomUUID().toString() : previous;
            if (!server.setBlockAndUpdate(pos, Exploration.CHEST.get().defaultBlockState())
                || !(server.getBlockEntity(pos) instanceof Tile tile)) return InteractionResult.FAIL;
            tile.bind(channel);
            if (!stack.is(Exploration.LINKED_SEAL.get())) {
                ItemStack linked = stack.transmuteCopy(Exploration.LINKED_SEAL.get());
                CustomData.update(DataComponents.CUSTOM_DATA, linked, data -> data.putString("channel", channel));
                player.setItemInHand(context.getHand(), linked);
            }
            ((net.minecraft.server.level.ServerPlayer) player).sendSystemMessage(Component.translatable(previous.isEmpty()
                ? "message.thaumicexploration.chest_first" : "message.thaumicexploration.chest_linked"), true);
            return InteractionResult.SUCCESS;
        }
    }
}
