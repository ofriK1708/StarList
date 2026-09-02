import { useState, useEffect } from "react";
import { Login } from "./components/Login";
import { GalaxyView } from "./components/GalaxyView";
import { TaskDashboard } from "./components/TaskDashboard";
import { AIChat } from "./components/AIChat";
import { Shop } from "./components/Shop";
import { CheckCircle, AlertCircle } from "lucide-react";
import { Statistics } from "./components/Statistics";
import { Profile } from "./components/Profile";
import { NavigationBar } from "./components/NavigationBar";
import { AddTaskModal } from "./components/AddTaskModal";
import { EditTaskModal } from "./components/EditTaskModal";
import { AddHabitModal } from "./components/AddHabitModal";
import { EditHabitModal } from "./components/EditHabitModal";
import { HabitTracker } from "./components/HabitTracker";
import confetti from 'canvas-confetti';
import { UserProvider, useUser } from "../context/UserContext";
import { tasksApi, TaskResponse, AddTaskRequest, UpdateTaskRequest } from "../services/taskApi";
import { habitsApi, HabitResponse, AddHabitRequest, UpdateHabitRequest } from "../services/habitsApi";
import { storeApi, ItemCatalogResponse } from "../services/storeApi";
import { aiApi } from "../services/aiApi";
import { achievementsApi, AchievementResponse } from "../services/achievementsApi";
import { markNewChat, readChatSince, turnsSince } from "../lib/aiChat";

type Screen = 'tasks' | 'habits' | 'galaxy' | 'chat' | 'profile' | 'shop' | 'statistics';

interface PlanetData {
  id: string;
  name: string;
  type: string;
  size: number;
  position: { x: number; y: number };
  imageUrl?: string;
}

interface ChatMessage {
  id: string;
  type: 'ai' | 'user' | 'task-suggestion';
  content: string;
  timestamp: Date;
  taskData?: any;
}

export interface ShopItemState {
  id: string;
  name: string;
  type: any;
  price: number;
  description: string;
  unlocked: boolean;
  imageUrl?: string;
  positionX?: number;
  positionY?: number;
  scale?: number;
}

// Positions spread across the full canvas (y >= 18 to stay below the header overlay).
// Designed for 180px visual planets (120px container + scale-150) on 1280×720+.
// Minimum ~200px center-to-center distance on a 1280×720 viewport.
const GALAXY_POSITIONS = [
  { x: 18, y: 20 },
  { x: 50, y: 18 },
  { x: 85, y: 18 },
  { x: 90, y: 50 },
  { x: 78, y: 80 },
  { x: 48, y: 88 },
  { x: 18, y: 78 },
  { x: 28, y: 50 },
  { x: 50, y: 58 },
  { x: 70, y: 38 },
];

function MainApp() {
  const { user, spendCoins } = useUser();
  const [localCoins, setLocalCoins] = useState(user?.totalCoins || 0);
  const coinBalance = localCoins;
  const [currentScreen, setCurrentScreen] = useState<Screen>('galaxy');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const triggerConfetti = () => {
    confetti({
      particleCount: 100,
      spread: 90,
      origin: { y: 0.6 },
      scalar: 2,
      gravity: 1.2,
      ticks: 150,
      colors: ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ec4899'],
    });
  };

  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [habits, setHabits] = useState<HabitResponse[]>([]);
  const [achievements, setAchievements] = useState<AchievementResponse[]>([]);
  const [shopItems, setShopItems] = useState<ShopItemState[]>([]);
  const [planets, setPlanets] = useState<PlanetData[]>([]);

  useEffect(() => {
    const loadData = async () => {
      try {
        const [fetchedTasks, fetchedHabits] = await Promise.all([
          tasksApi.getTasks(),
          habitsApi.getHabits(),
        ]);
        setTasks(fetchedTasks);
        setHabits(fetchedHabits);
      } catch (error) {
        console.error(error);
      }
      try {
        const fetchedAchievements = await achievementsApi.getMyAchievements();
        if (Array.isArray(fetchedAchievements)) setAchievements(fetchedAchievements);
      } catch (error) {
        console.error('Achievements fetch failed:', error);
      }
    };

    const loadStoreData = async () => {
      try {
        const [catalogItems, myItems] = await Promise.all([
          storeApi.getAvailableItems(),
          storeApi.getMyItems()
        ]);

        const ownedCatalogIds = new Set(myItems.map((item: any) => item.catalogItemId));

        const formattedShopItems: ShopItemState[] = catalogItems.map((item: any) => ({
          id: item.id.toString(),
          name: item.itemName,
          type: item.itemType || item.type || 'earth',
          price: item.costCoins,
          description: item.description || 'A celestial object',
          unlocked: ownedCatalogIds.has(item.id),
          imageUrl: item.imageUrl,
          positionX: item.positionX,
          positionY: item.positionY,
          scale: item.scale
        }));

        setShopItems(formattedShopItems);

        let nonSunIndex = 0;
        const myPlanetsData = myItems.map((myVal: any) => {
          const catalogInfo = catalogItems.find((c: any) => c.id === myVal.catalogItemId);
          const itemType = (catalogInfo?.itemType || catalogInfo?.type || 'planet').toLowerCase();
          const isSun = itemType === 'star' || itemType === 'sun';

          let baseSize = 100;
          let baseX: number;
          let baseY: number;

          if (isSun) {
            baseX = 8;
            baseY = 50;
          } else {
            const pos = GALAXY_POSITIONS[nonSunIndex % GALAXY_POSITIONS.length];
            baseX = pos.x;
            baseY = pos.y;
            nonSunIndex++;
          }

          if (catalogInfo?.scale && Number(catalogInfo.scale) > 0) baseSize = Number(catalogInfo.scale);

          return {
            id: myVal.id.toString(),
            name: catalogInfo?.itemName || 'Unknown Planet',
            type: itemType,
            size: baseSize,
            position: { x: baseX, y: baseY },
            imageUrl: catalogInfo?.imageUrl
          };
        });

        setPlanets(myPlanetsData);
      } catch (error) {
        console.error(error);
      }
    };

    loadData();
    if (user) loadStoreData();
  }, [user]);

  const handleTaskToggle = async (taskId: number) => {
    const task = tasks.find(t => t.taskId === taskId);
    if (!task || task.status === 'COMPLETED') return;
    try {
      const response = await tasksApi.completeTask(taskId);
      setTasks(tasks.map(t => t.taskId === taskId ? { ...t, status: 'COMPLETED' } : t));
      setLocalCoins(response.newTotalCoins);
      triggerConfetti();
      showToast(`Mission Accomplished! +${response.coinsEarned} coins`);
    } catch (error) {
      showToast("Error", "error");
    }
  };

  const handleTaskDelete = async (taskId: number) => {
    try {
      await tasksApi.deleteTask(taskId);
      setTasks(tasks.filter(task => task.taskId !== taskId));
    } catch (error) {
      console.error(error);
    }
  };

  const handleCreateNewTask = async (taskData: AddTaskRequest) => {
    try {
      const newTask = await tasksApi.createTask(taskData);
      setTasks([newTask, ...tasks]);
      setIsAddTaskModalOpen(false);
    } catch (error) {
      console.error(error);
    }
  };

  const handleUpdateTask = async (taskId: number, data: UpdateTaskRequest) => {
    try {
      const updated = await tasksApi.updateTask(taskId, data);
      setTasks(prev => prev.map(t => t.taskId === taskId ? updated : t));
      setIsEditTaskModalOpen(false);
      setTaskToEdit(null);
    } catch (error) {
      console.error(error);
      showToast("Couldn't update the task. Please try again.", "error");
    }
  };

  const [isEditHabitModalOpen, setIsEditHabitModalOpen] = useState(false);
  const [habitToEdit, setHabitToEdit] = useState<HabitResponse | null>(null);

  /** Re-fetches the full habit list so monthCompletions is always fresh. */
  const refreshHabits = async () => {
    const updated = await habitsApi.getHabits();
    setHabits(updated);
  };

  const handleHabitCheck = async (habitId: number) => {
    try {
      const response = await habitsApi.completeHabit(habitId);
      setLocalCoins(response.newTotalCoins);
      // Optimistic streak/date update so the button disables immediately,
      // then refresh to get the correct monthCompletions for the ring.
      setHabits(habits.map(habit => {
        if (habit.habitId === habitId) {
          const today = new Date().toISOString().split('T')[0];
          return { ...habit, currentStreak: response.currentStreak, bestStreak: response.bestStreak, lastCompletedDate: today, totalCompletions: habit.totalCompletions + 1 };
        }
        return habit;
      }));
      triggerConfetti();
      showToast(`Habit Logged! +${response.coinsEarned} coins`);
      await refreshHabits();
    } catch (error) {
      showToast("Error", "error");
    }
  };

  const handleAddHabit = async (habitData: AddHabitRequest) => {
    try {
      await habitsApi.createHabit(habitData);
      setIsAddHabitModalOpen(false);
      await refreshHabits();
    } catch (error) {
      console.error(error);
      showToast("Couldn't add the habit. Please try again.", "error");
      // Rethrow so the modal keeps the user's input instead of resetting to a blank form.
      throw error;
    }
  };

  const handleUpdateHabit = async (habitId: number, data: UpdateHabitRequest) => {
    try {
      await habitsApi.updateHabit(habitId, data);
      setIsEditHabitModalOpen(false);
      await refreshHabits();
    } catch (error) {
      console.error(error);
    }
  };

  const handleDeleteHabit = async (habitId: number) => {
    try {
      await habitsApi.deleteHabit(habitId);
      setHabits(habits.filter(habit => habit.habitId !== habitId));
    } catch (error) {
      console.error(error);
    }
  };

  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [chatHistoryLoaded, setChatHistoryLoaded] = useState(false);

  const [isAddTaskModalOpen, setIsAddTaskModalOpen] = useState(false);
  const [isEditTaskModalOpen, setIsEditTaskModalOpen] = useState(false);
  const [taskToEdit, setTaskToEdit] = useState<TaskResponse | null>(null);
  const [isAddHabitModalOpen, setIsAddHabitModalOpen] = useState(false);

  // Load chat history the first time the user opens the chat screen. The backend
  // stores every turn without a thread id, so we only restore turns from the
  // current chat (since the last "New Chat"), tracked client-side.
  useEffect(() => {
    if (currentScreen === 'chat' && !chatHistoryLoaded && user) {
      aiApi.getHistory().then((history) => {
        const historyMessages: ChatMessage[] = turnsSince(history, readChatSince())
          .flatMap((turn) => [
            { id: `u-${turn.conversationId}`, type: 'user' as const, content: turn.userMessage, timestamp: new Date(turn.createdAt) },
            { id: `a-${turn.conversationId}`, type: 'ai' as const, content: turn.aiResponse, timestamp: new Date(turn.createdAt) },
          ]);
        if (historyMessages.length > 0) {
          setChatMessages(historyMessages);
        }
        setChatHistoryLoaded(true);
      }).catch(() => setChatHistoryLoaded(true));
    }
  }, [currentScreen, chatHistoryLoaded, user]);

  const appendAiMessage = (content: string) => {
    setChatMessages(prev => [...prev, {
      id: Date.now().toString(),
      type: 'ai',
      content,
      timestamp: new Date(),
    }]);
  };

  const handleSendMessage = async (message: string, newConversation?: boolean) => {
    if (newConversation) {
      // Remember when this chat started so a refresh won't replay earlier chats.
      markNewChat();
    }
    if (!message.trim()) {
      // Empty priming call from "New Chat" — just clear the local thread.
      if (newConversation) setChatMessages([]);
      return;
    }

    setChatMessages(prev => [...prev, {
      id: Date.now().toString(),
      type: 'user',
      content: message,
      timestamp: new Date(),
    }]);
    setIsAiLoading(true);

    try {
      const response = await aiApi.sendMessage({
        message,
        newConversation: !!newConversation,
        userTimezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      });
      if (newConversation) {
        // Anchor the "current chat" marker to this first turn's server timestamp
        // (a small backstep absorbs client/server clock skew).
        const anchor = new Date(response.createdAt).getTime();
        if (Number.isFinite(anchor)) markNewChat(anchor - 1000);
      }
      appendAiMessage(response.aiMessage);
      if (response.tasksCreated > 0) {
        const updatedTasks = await tasksApi.getTasks();
        setTasks(updatedTasks);
        showToast(`AI created ${response.tasksCreated} task${response.tasksCreated > 1 ? 's' : ''} for you!`);
      }
      if (response.habitsCreated > 0) {
        const updatedHabits = await habitsApi.getHabits();
        setHabits(updatedHabits);
        showToast(`AI created ${response.habitsCreated} habit${response.habitsCreated > 1 ? 's' : ''} for you!`);
      }
    } catch {
      appendAiMessage("Sorry, I'm having trouble connecting right now. Please try again.");
    } finally {
      setIsAiLoading(false);
    }
  };

  const handleDailyBriefing = async () => {
    setIsAiLoading(true);
    try {
      const response = await aiApi.getDailyBriefing();
      appendAiMessage(response.aiMessage);
    } catch {
      appendAiMessage("Couldn't load your daily briefing. Please try again.");
    } finally {
      setIsAiLoading(false);
    }
  };

  const handlePurchase = async (itemId: string) => {
    const item = shopItems.find(i => i.id === itemId);
    if (!item || item.unlocked || coinBalance < item.price) return;

    try {
      const response = await storeApi.buyItem(Number(itemId));
      setLocalCoins(response.newTotalCoins);
      spendCoins(item.price);
      setShopItems(prevItems => prevItems.map(i => i.id === itemId ? { ...i, unlocked: true } : i));

      const itemType = (item.type || 'planet').toLowerCase();
      const isSun = itemType === 'star' || itemType === 'sun';

      let baseSize = 100;
      let baseX: number;
      let baseY: number;

      if (isSun) {
        baseX = 8;
        baseY = 50;
      } else {
        const nonSunCount = planets.filter(p => p.type !== 'sun' && p.type !== 'star').length;
        const pos = GALAXY_POSITIONS[nonSunCount % GALAXY_POSITIONS.length];
        baseX = pos.x;
        baseY = pos.y;
      }

      if (item.scale && Number(item.scale) > 0) baseSize = Number(item.scale);

      const newPlanet = {
        id: Date.now().toString(),
        name: item.name,
        type: itemType,
        size: baseSize,
        position: { x: baseX, y: baseY },
        imageUrl: item.imageUrl
      };

      setPlanets(prevPlanets => [...prevPlanets, newPlanet]);
      showToast(`${item.name} Unlocked!`);
    } catch (error) {
      console.error(error);
    }
  };

  let spentCoins = 0;
  for (const item of shopItems) {
    if (item.unlocked) spentCoins += item.price;
  }

  return (
      <div className="h-screen w-full flex flex-col bg-slate-900 overflow-hidden relative">
        {toast && (
            <div className="fixed top-6 left-1/2 transform -translate-x-1/2 z-[100] animate-in slide-in-from-top-4 fade-in duration-300">
              <div className={`flex items-center gap-3 px-5 py-3 rounded-2xl shadow-2xl border ${ toast.type === 'success' ? 'bg-green-500/20 border-green-500/30 text-green-100' : 'bg-red-500/20 border-red-500/30 text-red-100' } backdrop-blur-md`}>
                <CheckCircle className="w-5 h-5" />
                <p className="font-medium text-sm">{toast.message}</p>
              </div>
            </div>
        )}

        <div className="flex-1 overflow-hidden">
          {currentScreen === 'galaxy' && <GalaxyView coinBalance={coinBalance} planets={planets} />}
          {currentScreen === 'tasks' && <TaskDashboard tasks={tasks} onTaskToggle={handleTaskToggle} onQuickAdd={() => setIsAddTaskModalOpen(true)} onOpenChat={() => setCurrentScreen('chat')} onTaskEdit={(t) => { setTaskToEdit(t); setIsEditTaskModalOpen(true); }} onTaskDelete={handleTaskDelete} />}
          {currentScreen === 'habits' && <HabitTracker habits={habits.filter(h => {
            if (h.frequency !== 'MULTI_DAY') return true;
            // JS getDay(): 0=Sun…6=Sat → convert to ISO 1=Mon…7=Sun
            const jsDay = new Date().getDay();
            const isoDay = jsDay === 0 ? 7 : jsDay;
            return h.scheduledDaysOfWeek?.includes(isoDay) ?? false;
          })} onHabitCheck={handleHabitCheck} onAddHabitClick={() => setIsAddHabitModalOpen(true)} onEditHabitClick={(h) => { setHabitToEdit(h); setIsEditHabitModalOpen(true); }} onDeleteHabit={handleDeleteHabit} />}
          {currentScreen === 'chat' && <AIChat messages={chatMessages} onSendMessage={handleSendMessage} onAddTask={() => {}} onDailyBriefing={handleDailyBriefing} isLoading={isAiLoading} />}
          {currentScreen === 'shop' && <Shop items={shopItems as any} coinBalance={coinBalance} onPurchase={handlePurchase} />}
          {currentScreen === 'statistics' && <Statistics tasks={tasks as any} habits={habits} totalCoinsEarned={coinBalance + spentCoins} currentCoins={coinBalance} shopItems={shopItems} />}
          {currentScreen === 'profile' && <Profile user={{ name: user?.displayName || 'Explorer', email: user?.email || '', achievements: achievements.map(a => ({ id: a.id, name: a.name, icon: a.icon, description: a.description, unlocked: a.unlocked })) }} coinBalance={coinBalance} />}
        </div>

        <AddTaskModal isOpen={isAddTaskModalOpen} onClose={() => setIsAddTaskModalOpen(false)} onAdd={handleCreateNewTask} />
        <EditTaskModal isOpen={isEditTaskModalOpen} onClose={() => { setIsEditTaskModalOpen(false); setTaskToEdit(null); }} taskToEdit={taskToEdit} onUpdate={handleUpdateTask} />
        <AddHabitModal isOpen={isAddHabitModalOpen} onClose={() => setIsAddHabitModalOpen(false)} onAdd={handleAddHabit} />
        <EditHabitModal isOpen={isEditHabitModalOpen} onClose={() => setIsEditHabitModalOpen(false)} habitToEdit={habitToEdit} onUpdate={handleUpdateHabit} />
        <NavigationBar currentScreen={currentScreen} onNavigate={setCurrentScreen} />
      </div>
  );
}

function AppWrapper() {
  const { user, isLoading } = useUser();
  if (isLoading) return <div className="h-screen w-full flex items-center justify-center bg-slate-950 text-white">Loading...</div>;
  if (!user) return <Login />;
  return <MainApp />;
}

export default function App() { return ( <UserProvider> <AppWrapper /> </UserProvider> ); }