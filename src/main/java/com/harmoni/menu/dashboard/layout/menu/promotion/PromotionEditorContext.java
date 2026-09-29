package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.vaadin.flow.component.tabs.Tab;
import lombok.Builder;

import java.io.Serializable;

/**
 * The per-editor state {@link PromotionListView} hands to the promotion editor when it
 * opens a tab.
 *
 * <p>These four values travel together because they only make sense together: the
 * {@link FormAction} says which button the editor offers, the {@code promotionDto} is
 * what that action applies to, and the {@code Tab} and {@code TabManager} are the tab
 * the action closes when it succeeds. The three call sites - create, edit and status
 * transition - differ only in which of the four they fill in, and passed positionally
 * the only thing marking which was which was its position.</p>
 *
 * <p>Worth noting for whoever edits this next: {@code accessService} sat as the eighth
 * argument to the editor and the fourth in {@link PromotionForm}'s constructor. The
 * editor forwarded its arguments to the form, so the two orderings had to agree by hand.</p>
 *
 * <p>The four exchanged services stay separate constructor arguments. They are Spring
 * beans; folding them in here would make this a description of a whole application
 * rather than of one editor.</p>
 *
 * <p>A plain carrier, deliberately without convenience accessors. {@code formAction} in
 * particular is read raw by {@code PromotionForm.isNewPromotion()}, which treats a null
 * action differently from {@code CREATE} on purpose, and a defaulting accessor would
 * quietly close that gap.</p>
 *
 * @param tabManager   closes {@code currentTab} and reselects the list tab
 * @param currentTab   the tab this editor was opened on
 * @param formAction   which action the editor was opened for
 * @param promotionDto the promotion being edited, or {@code null} when creating
 */
@Builder
public record PromotionEditorContext(TabManager tabManager,
                                     Tab currentTab,
                                     FormAction formAction,
                                     PromotionDto promotionDto) implements Serializable {
}
