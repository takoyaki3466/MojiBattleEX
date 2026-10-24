package jp.takoa.mojibattle.event;

import java.util.function.Consumer;

/**
 * MODへ公開する登録ライフサイクルの購読専用境界。
 *
 * <p>イベントの発行権限を公開せず、ホストが配送する登録イベントへの購読だけを許可する。</p>
 */
public interface RegistrationBus {
    /**
     * この購読境界を所有するMOD namespace。ホスト内部Busではnull。
     */
    default String ownerNamespace() {
        return null;
    }

    /**
     * 指定したイベント型のリスナーを登録する。
     *
     * @param eventType 完全一致で受信するイベント型
     * @param listener  同期的に呼び出されるリスナー
     * @return 購読解除用ハンドル
     */
    <E> EventSubscription subscribe(Class<E> eventType, Consumer<? super E> listener);
}
